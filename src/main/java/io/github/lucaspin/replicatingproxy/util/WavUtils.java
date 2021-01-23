package io.github.lucaspin.replicatingproxy.util;

import static java.nio.charset.StandardCharsets.US_ASCII;

public class WavUtils {

    public static final int PCM_WAV_HEADER_SIZE = 44;
    private static final byte[] RIFF = "RIFF".getBytes(US_ASCII);
    private static final byte[] WAVE = "WAVE".getBytes(US_ASCII);
    private static final byte[] FMT = "fmt ".getBytes(US_ASCII); // the space is intentional
    private static final byte[] DATA = "data".getBytes(US_ASCII);
    private static final int PCM_CHUNK_SIZE = 16;
    private static final short PCM_FORMAT = 1;

    public static byte[] buildHeader(short numChannels, int sampleRate, short bitsPerSample, int numSamples) {
        WavHeader header = new WavHeader();

        int subChunk2Size = numSamples * numChannels * bitsPerSample / Byte.SIZE;

        // chunk ID
        header.writeBytes(RIFF);

        // chunk size (size of whole file minus the 8 bytes from this and the previous thing
        int chunkSize = subChunk2Size + PCM_WAV_HEADER_SIZE - 8;
        header.writeIntLittleEndian(chunkSize);

        // format
        header.writeBytes(WAVE);

        // subchunk 1 ID
        header.writeBytes(FMT);

        // subchunk 1 size
        header.writeIntLittleEndian(PCM_CHUNK_SIZE);

        // audio format
        header.writeShortLittleEndian(PCM_FORMAT);

        // num channels
        header.writeShortLittleEndian(numChannels);

        // sample rate
        header.writeIntLittleEndian(sampleRate);

        // byte rate
        int byteRate = sampleRate * numChannels * bitsPerSample / Byte.SIZE;
        header.writeIntLittleEndian(byteRate);

        // block align
        short blockAlign = (short) (numChannels * bitsPerSample / Byte.SIZE);
        header.writeShortLittleEndian(blockAlign);

        // bits per sample
        header.writeShortLittleEndian(bitsPerSample);

        // subchunk 2 ID
        header.writeBytes(DATA);

        // subchunk 2 size
        header.writeIntLittleEndian(subChunk2Size);

        return header.bytes;
    }

    private static class WavHeader {
        private final byte[] bytes = new byte[PCM_WAV_HEADER_SIZE];
        private int offset = 0;

        private void writeBytes(byte[] src) {
            System.arraycopy(src, 0, bytes, offset, src.length);
            offset += src.length;
        }

        private void writeIntLittleEndian(int toWrite) {
            WavUtils.writeIntLittleEndian(toWrite, bytes, offset);
            offset += Integer.BYTES;
        }

        private void writeShortLittleEndian(short toWrite) {
            bytes[offset++] = (byte) toWrite;
            bytes[offset++] = (byte) (toWrite >>> 8);
        }
    }

    public static void writeIntLittleEndian(int toWrite, byte[] target, int offset) {
        target[offset++] = (byte) toWrite;
        target[offset++] = (byte) (toWrite >>> 8);
        target[offset++] = (byte) (toWrite >>> 16);
        target[offset] = (byte) (toWrite >>> 24);
    }
}