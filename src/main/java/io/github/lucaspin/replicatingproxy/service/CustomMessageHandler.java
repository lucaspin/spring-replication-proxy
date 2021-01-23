package io.github.lucaspin.replicatingproxy.service;

import io.github.lucaspin.replicatingproxy.util.RTPPacketParser;
import io.github.lucaspin.replicatingproxy.util.WavUtils;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.engineio.client.transports.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.Message;
import java.net.URISyntaxException;
import java.util.function.Consumer;

import static io.github.lucaspin.replicatingproxy.util.RTPPacketParser.parsePacket;

public class CustomMessageHandler extends AbstractMessageHandler {

    private static final Logger LOG = LoggerFactory.getLogger(CustomMessageHandler.class);
    private static final String WS_SERVER = "http://127.0.0.1:4010";
    private static final short MONO = 1;
    private static final short STEREO = 2;
    public static final int TEN_SECONDS = 10;
    public static final short BITS_PER_SAMPLE = 16;
    public static final int EIGHT_THOUSAND = 8000;
    public static final int FORTY_FOUR_THOUSAND = 44100;

    private final Socket webSocket;
    private final Integer port;
    private final Consumer<Integer> whenDoneConsumer;

    public CustomMessageHandler(Consumer<Integer> whenDoneConsumer, Integer port) throws URISyntaxException {
        this.port = port;
        this.whenDoneConsumer = whenDoneConsumer;
        this.webSocket = IO.socket(WS_SERVER, IO.Options.builder()
                .setTransports(new String[]{ WebSocket.NAME })
                .build());
        init();
    }

    private void init() {
        webSocket.on(Socket.EVENT_CONNECT, args -> {
            LOG.info("Connected to {}", WS_SERVER);
            webSocket.on("message", message -> LOG.info("Received {}", message));
            webSocket.on("error", x -> LOG.error("Socket error"));
            webSocket.on("close", x -> {
                LOG.info("Socket close");
                whenDoneConsumer.accept(port);
            });

            int numSamples = FORTY_FOUR_THOUSAND * TEN_SECONDS;
            webSocket.send(WavUtils.buildHeader(STEREO, FORTY_FOUR_THOUSAND, BITS_PER_SAMPLE, numSamples));
        });

        webSocket.on(Socket.EVENT_CONNECT_ERROR, e -> {
            LOG.error("Connection failed: {}", e);
            whenDoneConsumer.accept(port);
        });

        LOG.info("Connecting to {}", WS_SERVER);
        webSocket.connect();
    }

    @Override
    protected void handleMessageInternal(Message<?> message) {
        if (webSocket.connected()) {
            RTPPacketParser.RTPPacket packet = parsePacket((byte[]) message.getPayload());
            webSocket.send(packet.getPayload());
        } else {
            LOG.error("Websocket is not connected to {}", WS_SERVER);
        }
    }
}
