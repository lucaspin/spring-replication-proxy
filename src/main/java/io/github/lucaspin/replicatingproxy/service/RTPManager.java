package io.github.lucaspin.replicatingproxy.service;

import io.github.lucaspin.replicatingproxy.util.RTPPacketParser;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RTPManager {
    private static final String WS_SERVER = "http://127.0.0.1:4010";
    private static final int MAX_PACKETS_BEFORE_FLUSHING = 128;

    /**
     * This is a header for a pcm_s16le, 44100 Hz, 2 channels, 16 bits per sample media stream
     */
    private static final byte[] PCMU_WAV_HEADER = new byte[]{
            82, 73, 70, 70, -60, -22, 26, 0,
            87, 65, 86, 69, 102, 109, 116, 32,
            16, 0, 0, 0, 1, 0, 2, 0,
            68, -84, 0, 0, 16, -79, 2, 0,
            4, 0, 16, 0, 100, 97, 116, 97,
            -96, -22, 26, 0
    };

    private final Map<Integer, SyncSourceStatus> syncSources = new HashMap<>();
    private final Clock clock = Clock.systemUTC();

    public synchronized void onPacketReceived(RTPPacketParser.RTPPacket packet) {
        if (syncSources.containsKey(packet.getSynchronizationSourceId())) {
            SyncSourceStatus syncSourceStatus = syncSources.get(packet.getSynchronizationSourceId());
            synchronized (syncSourceStatus.getLock()) {
                syncSourceStatus.addPacket(packet);
                syncSourceStatus.setLastPacketReceivedAt(clock.instant());
                if (syncSourceStatus.getPackets().size() > MAX_PACKETS_BEFORE_FLUSHING) {
                    syncSourceStatus.flush();
                }
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
                socket.send(PCMU_WAV_HEADER);
            });

            socket.on(Socket.EVENT_CONNECT_ERROR, e -> log.error("Connection failed: {}", e));
            log.info("Connecting to {}", WS_SERVER);
            return socket.connect();
        } catch (URISyntaxException e) {
            log.error("Error initializing socket", e);
            return null;
        }
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
            log.info("Flushing packets for {}", syncSourceId);
            synchronized (lock) {
                packets = packets.stream().sorted().collect(Collectors.toList());
                packets.forEach(packet -> webSocket.send(packet.getPayload()));
                packets = new ArrayList<>();
            }
        }
    }
}
