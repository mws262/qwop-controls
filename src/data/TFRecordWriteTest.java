package data;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.tensorflow.example.Example;
import org.tensorflow.example.Feature;
import org.tensorflow.example.Features;
import org.tensorflow.example.FloatList;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.LittleEndianDataOutputStream;

public class TFRecordWriteTest {

	public static void main(String[] args) {
		//		
		//					// Validate -- not needed during batch run.
		//					Example dataValidate = null;
		//					String fileName = "denseData_2017-11-07_11-23-45.tfrecords";
		//					try {
		//						FileInputStream fIn = new FileInputStream(fileName);
		//						
		//						dataValidate = Example.parseFrom(new FileInputStream(fileName));
		//					} catch (IOException e) {
		//						// TODO Auto-generated catch block
		//						e.printStackTrace();
		//					}
		//					DenseData.State state1 = dataValidate.getDenseData(0).getState(10);
		//					float dx1 = state1.getBody().getDx();
		//					System.out.println("From new file: " + dx1 + ". From original data: " + denseDat.get(0).getState()[10].body.dx);
		//					
		//					
		//					DenseData.State state2 = dataValidate.getDenseData(10).getState(3);
		//					float th1 = state2.getHead().getTh();
		//					System.out.println("From new file: " + th1 + ". From original data: " + denseDat.get(10).getState()[3].head.th);
		//					break;


		Example.Builder ex = Example.newBuilder();
		Features.Builder feats = Features.newBuilder();
		Feature.Builder feat = Feature.newBuilder();
		FloatList.Builder featVals = FloatList.newBuilder();
		featVals.addValue(1.f);
		featVals.addValue(2.f);
		featVals.addValue(3.f);
		featVals.addValue(4.f);
		featVals.addValue(5.f);
		featVals.addValue(6.f);

		feat.setFloatList(featVals.build());
		feats.putFeature("x", feat.build());
		ex.setFeatures(feats.build());
		System.out.println(ex.build().toString());

		byte[] exSerialized = ex.build().toByteArray();
		byte[] length = LittleEndianEncoding.encodeLong(exSerialized.length);
		byte[] crcLength = LittleEndianEncoding.encodeInt(CRC32.mask(CRC32.hash(length)));
		byte[] crcEx = LittleEndianEncoding.encodeInt(CRC32.mask(CRC32.hash(exSerialized)));

		FileOutputStream stream = null;
		try {
			stream = new FileOutputStream(new File("test.NEWNEWNEW"));
			stream.write(length);
			stream.write(crcLength);
			stream.write(exSerialized);
			stream.write(crcEx);

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	// Code shamelessly ripped and converted from scala from : https://stackoverflow.com/questions/34711264/pure-java-scala-code-for-writing-tensorflow-tfrecords-data-file
	// Thank you kind stranger.
	public static class CRC32 {
		private static final int MASK_DELTA = 0xa282ead8;

		public static int hash(byte[] input) {
			HashFunction hf = Hashing.crc32c();
			return hf.hashBytes(input).asInt();
		}

		public static int mask(int crc) {
			// Rotate right by 15 bits and add a constant.
			return ((crc >>> 15) | (crc << 17)) + MASK_DELTA;
		}
		public static int unmask(int maskedCrc) {
			int rot = maskedCrc - MASK_DELTA;
			return ((rot >>> 17) | (rot << 15));
		}
	}

	public static class LittleEndianEncoding {
		public static byte[] encodeLong(long input) {
			ByteArrayOutputStream bStream = new ByteArrayOutputStream();
			LittleEndianDataOutputStream leStream = new LittleEndianDataOutputStream(bStream);
			try {
				leStream.writeLong(input);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return bStream.toByteArray();
		}

		public static byte[] encodeInt(int input) {
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
