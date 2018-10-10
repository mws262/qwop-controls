package data;

import org.apache.hadoop.util.PureJavaCrc32C;

import java.util.zip.Checksum;

class Crc32C implements Checksum {
    private static final int MASK_DELTA = 0xa282ead8;
    private PureJavaCrc32C crc32C;

    public static int maskedCrc32c(byte[] data) {
        return maskedCrc32c(data, 0, data.length);
    }

    private static int maskedCrc32c(byte[] data, int offset, int length) {
        Crc32C crc32c = new Crc32C();
        crc32c.update(data, offset, length);
        return crc32c.getMaskedValue();
    }

    /**
     * Return a masked representation of crc.
     *
     * Motivation: it is problematic to compute the CRC of a string that
     * contains embedded CRCs.  Therefore we recommend that CRCs stored
     * somewhere (e.g., in files) should be masked before being stored.
     */
    private static int mask(int crc) {
        // Rotate right by 15 bits and add a constant.
        return ((crc >>> 15) | (crc << 17)) + MASK_DELTA;
    }

    /**
     * Return the crc whose masked representation is masked_crc.
     */
    public static int unmask(int maskedCrc) {
        int rot = maskedCrc - MASK_DELTA;
        return ((rot >>> 17) | (rot << 15));
    }

    private Crc32C() {
        crc32C = new PureJavaCrc32C();
    }

    private int getMaskedValue() {
        return mask(getIntValue());
    }

    private int getIntValue() {
        return (int) getValue();
    }

    @Override
    public void update(int b) {
        crc32C.update(b);
    }

    @Override
    public void update(byte[] b, int off, int len) {
        crc32C.update(b, off, len);
    }

    @Override
    public long getValue() {
        return crc32C.getValue();
    }

    @Override
    public void reset() {
        crc32C.reset();
    }
}