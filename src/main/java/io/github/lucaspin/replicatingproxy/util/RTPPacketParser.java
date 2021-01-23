package io.github.lucaspin.replicatingproxy.util;

import lombok.Builder;
import lombok.Getter;
import java.nio.ByteBuffer;
import java.util.Arrays;

import lombok.ToString;

public class RTPPacketParser {
    public static RTPPacket parsePacket(byte[] packet) {
        return RTPPacket.builder()
                .version((packet[0] & 0b11000000) >>> 6)
                .padding(((packet[0] & 0b00100000) >> 5) == 1)
                .extension(((packet[0] & 0b00010000) >> 4) == 1)
                .contributingSourcesCount(packet[0] & 0b00001111)
                .marker(((packet[1] & 0b10000000) >> 7) == 1)
                .payloadType(packet[1] & 0b01111111)
                .sequenceNumber(ByteBuffer.wrap(packet, 2, 2).getShort())
                .timestamp(ByteBuffer.wrap(packet, 4, 4).getInt())
                .synchronizationSourceId(ByteBuffer.wrap(packet, 8, 4).getInt())
                .payload(Arrays.copyOfRange(packet, 12, packet.length))
                .build();
    }

    @Getter
    @Builder
    @ToString
    static public class RTPPacket {
        private final int version;
        private final boolean padding;
        private final boolean extension;
        private final int contributingSourcesCount;
        private final boolean marker;
        private final int payloadType;
        private final int sequenceNumber;
        private final int timestamp;
        private final int synchronizationSourceId;
        private final byte[] payload;
    }
}
