package data;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.LittleEndianDataOutputStream;

/* Simple writer to TFRecord format. This is patched together from fragments of code I've found around.
 * The biggest note: you still need to do everything in Protobuf form. This only does the writing to
 * file part of the process.
 *  e.g. pass in ex.build().toByteArray()
 */
public class TFRecordWriter {

    public static void writeToStream(byte[] serializedExample, FileOutputStream fos) throws IOException {
        byte[] length = LittleEndianEncoding.encodeLong(serializedExample.length);
        byte[] crcLength = LittleEndianEncoding.encodeInt(CRC32.mask(CRC32.hash(length)));
        byte[] crcEx = LittleEndianEncoding.encodeInt(CRC32.mask(CRC32.hash(serializedExample)));

        fos.write(length);
        fos.write(crcLength);
        fos.write(serializedExample);
        fos.write(crcEx);
    }

    // Code shamelessly ripped and converted from scala from : https://stackoverflow
	// .com/questions/34711264/pure-java-scala-code-for-writing-tensorflow-tfrecords-data-file
    // Thank you kind stranger.
    @SuppressWarnings("UnstableApiUsage")
    protected static class CRC32 {
        private static final int MASK_DELTA = 0xa282ead8;

        private static int hash(byte[] input) {
            HashFunction hf = Hashing.crc32c();
            return hf.hashBytes(input).asInt();
        }

        private static int mask(int crc) {
            // Rotate right by 15 bits and add a constant.
            return ((crc >>> 15) | (crc << 17)) + MASK_DELTA;
        }

        private static int unmask(int maskedCrc) {
            int rot = maskedCrc - MASK_DELTA;
            return ((rot >>> 17) | (rot << 15));
        }
    }

    private static class LittleEndianEncoding {
        private static byte[] encodeLong(long input) {
            ByteArrayOutputStream bStream = new ByteArrayOutputStream();
            LittleEndianDataOutputStream leStream = new LittleEndianDataOutputStream(bStream);
            try {
                leStream.writeLong(input);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bStream.toByteArray();
        }

        private static byte[] encodeInt(int input) {
            ByteArrayOutputStream bStream = new ByteArrayOutputStream();
            LittleEndianDataOutputStream leStream = new LittleEndianDataOutputStream(bStream);
            try {
                leStream.writeInt(input);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bStream.toByteArray();
        }
    }
}
