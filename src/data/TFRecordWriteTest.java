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

		FileOutputStream stream = null;
		try {
			stream = new FileOutputStream(new File("test.NEWNEWNEW"));
			TFRecordWriter.writeToStream(ex.build().toByteArray(), stream);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
