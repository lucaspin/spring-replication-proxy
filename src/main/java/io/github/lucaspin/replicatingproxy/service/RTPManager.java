package io.github.lucaspin.replicatingproxy.service;

import io.github.lucaspin.replicatingproxy.util.RTPPacketParser;
import io.github.lucaspin.replicatingproxy.util.WavUtils;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.engineio.client.transports.WebSocket;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RTPManager {
    private static final String WS_SERVER = "http://127.0.0.1:4010";

    private static final short MONO = 1;
    private static final short STEREO = 2;
    public static final int TEN_SECONDS = 10;
    public static final short BITS_PER_SAMPLE = 16;
    public static final int EIGHT_THOUSAND = 8000;
    public static final int FORTY_FOUR_THOUSAND = 44100;
    public static final Duration MAX_WAIT_BEFORE_FLUSH = Duration.ofSeconds(5);

    private final Map<Integer, SyncSourceStatus> syncSources = new HashMap<>();
    private final Clock clock = Clock.systemUTC();

    public synchronized void onPacketReceived(RTPPacketParser.RTPPacket packet) {
        if (syncSources.containsKey(packet.getSynchronizationSourceId())) {
            SyncSourceStatus syncSourceStatus = syncSources.get(packet.getSynchronizationSourceId());
            synchronized (syncSourceStatus.getLock()) {
                syncSourceStatus.addPacket(packet);
                syncSourceStatus.setLastPacketReceivedAt(clock.instant());
            }
        } else {
            ArrayList<RTPPacketParser.RTPPacket> packets = new ArrayList<>();
            packets.add(packet);
            syncSources.put(packet.getSynchronizationSourceId(), SyncSourceStatus.builder()
                    .syncSourceId(packet.getSynchronizationSourceId())
                    .packets(packets)
                    .webSocket(initializeSocket())
                    .lastPacketReceivedAt(clock.instant())
                    .lock(new Object())
                    .build());
        }
    }

    private Socket initializeSocket() {
        try {
            Socket socket = IO.socket(WS_SERVER, IO.Options.builder()
                    .setTransports(new String[]{ WebSocket.NAME })
                    .build());

            socket.on(Socket.EVENT_CONNECT, args -> {
                log.info("Connected to {}", WS_SERVER);
                int numSamples = FORTY_FOUR_THOUSAND * TEN_SECONDS;
                socket.send(WavUtils.buildHeader(STEREO, FORTY_FOUR_THOUSAND, BITS_PER_SAMPLE, numSamples));
            });

            socket.on(Socket.EVENT_CONNECT_ERROR, e -> log.error("Connection failed: {}", e));
            log.info("Connecting to {}", WS_SERVER);
            return socket.connect();
        } catch (URISyntaxException e) {
            log.error("Error initializing socket", e);
            return null;
        }
    }

    @Scheduled(fixedRateString = "PT5S")
    public void flushPackets() {
        syncSources.forEach((syncSourceId, status) -> {
            if (status.getPackets().isEmpty()) {
                log.info("No packets to send for {}", syncSourceId);
            } else {
                synchronized (status.getLock()) {
                    Instant flushAt = status.getLastPacketReceivedAt().plus(MAX_WAIT_BEFORE_FLUSH);
                    if (clock.instant().isAfter(flushAt)) {
                        log.info("Last packet received was at {} - flushing", status.getLastPacketReceivedAt());
                        status.flush();
                    } else {
                        log.info("Last packet received was at {} - waiting", status.getLastPacketReceivedAt());
                    }
                }
            }
        });
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    static class SyncSourceStatus {
        private int syncSourceId;
        private List<RTPPacketParser.RTPPacket> packets;
        private Socket webSocket;
        private Instant lastPacketReceivedAt;
        private final Object lock;

        public void addPacket(RTPPacketParser.RTPPacket packet) {
            packets.add(packet);
        }

        public void flush() {
            synchronized (lock) {
                packets = packets.stream().sorted().collect(Collectors.toList());
                packets.forEach(packet -> webSocket.send(packet.getPayload()));
                packets = new ArrayList<>();
            }
        }
    }
}
