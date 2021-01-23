package io.github.lucaspin.replicatingproxy.service;

import io.github.lucaspin.replicatingproxy.util.RTPPacketParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.Message;

import static io.github.lucaspin.replicatingproxy.util.RTPPacketParser.parsePacket;

@Slf4j
@RequiredArgsConstructor
public class CustomMessageHandler extends AbstractMessageHandler {

    private final RTPManager rtpManager;

    @Override
    protected void handleMessageInternal(Message<?> message) {
        RTPPacketParser.RTPPacket packet = parsePacket((byte[]) message.getPayload());
        rtpManager.onPacketReceived(packet);
    }
}
