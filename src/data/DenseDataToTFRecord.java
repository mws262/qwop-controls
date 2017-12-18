package data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import main.State;
import main.Action;


public class DenseDataToTFRecord {

	static String sourceDir = ".";
	static String inFileExt = "SaveableDenseData";

	public static void main(String[] args) {

		/** Grab input files. **/
		System.out.println("Identifying input files...");
		File inDir = new File(sourceDir);
		if (!inDir.exists()) throw new RuntimeException("Input directory does not exist here: " + inDir.getName());

		double megabyteCount = 0;
		ArrayList<File> inFiles = new ArrayList<File>();
		for(File file: inDir.listFiles()) {	
			if (!file.isDirectory()) {
				String extension = "";
				// Get only files with the correct file extension.
				int i = file.getName().lastIndexOf('.');
				if (i > 0) {
					extension = file.getName().substring(i+1);
				}
				if (extension.equals(inFileExt)) {
					inFiles.add(file);	
					megabyteCount += file.length()/1.0e6;
				}else {
					System.out.println("Ignoring file in input directory: " + file.getName());
				}
			}
		}

		System.out.println("Found " + inFiles.size() + " input files with the extension " + inFileExt + ".");
		System.out.println("Total input size: " + Math.round(megabyteCount*10)/10. + " MB.");

		System.out.println("done");


		SaveableFileIO<SaveableDenseData> inFileLoader = new SaveableFileIO<SaveableDenseData>();
		int count = 0;
		for (File file : inFiles) {
			ArrayList<SaveableDenseData> denseDat = inFileLoader.loadObjectsOrdered(file.getAbsolutePath());
			System.out.print("Beginning to package " + file.getName() + ". ");
			String fileOutName = file.getName().substring(0, file.getName().lastIndexOf('.')) + ".proto";
			try {
				convertToProtobuf(denseDat,fileOutName);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			count++;
			System.out.println("Done. " + count + "/" + inFiles.size());
			
//			// Validate -- not needed during batch run.
//			DataSet dataValidate = null;
//			try {
//				FileInputStream fIn = new FileInputStream(fileOutName);
//				
//				dataValidate = DataSet.parseFrom(new FileInputStream(fileOutName));
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			DenseData.State state1 = dataValidate.getDenseData(0).getState(10);
//			float dx1 = state1.getBody().getDx();
//			System.out.println("From new file: " + dx1 + ". From original data: " + denseDat.get(0).getState()[10].body.dx);
//			
//			
//			DenseData.State state2 = dataValidate.getDenseData(10).getState(3);
//			float th1 = state2.getHead().getTh();
//			System.out.println("From new file: " + th1 + ". From original data: " + denseDat.get(10).getState()[3].head.th);
//			break;
//			
		}
	}


	public static void convertToProtobuf(List<SaveableDenseData> denseData, String fileName) throws IOException {

		//DataSet.Builder set = DataSet.newBuilder();
		
		for (SaveableDenseData dat : denseData) {
			SequenceExample.Builder seqEx = SequenceExample.newBuilder();
			
			// Use timestamp as unique run identifier.
			Int64List.Builder identifier = Int64List.newBuilder();
			identifier.addValue(System.currentTimeMillis());
			Feature.Builder identifierFeat = Feature.newBuilder(); // It gets its own feature.
			identifierFeat.setInt64List(identifier.build());
			
			seqEx.setContext(identifierFeat.build());
			FeatureList.Builder feats = FeatureList.newBuilder();
		
			for (State st : dat.getState()) {
				Feature.Builder feat = Feature.newBuilder();
				FloatList.Builder slist = FloatList.newBuilder();
				
				

				//DenseData.State.Builder state = DenseData.State.newBuilder();
				
				// Do for body:
				slist.addValue(st.body.x);
				slist.addValue(st.body.y);
				slist.addValue(st.body.th);
				slist.addValue(st.body.dx);
				slist.addValue(st.body.dy);
				slist.addValue(st.body.dth);

				// For head:
				slist.addValue(st.head.x);
				slist.addValue(st.head.y);
				slist.addValue(st.head.th);
				slist.addValue(st.head.dx);
				slist.addValue(st.head.dy);
				slist.addValue(st.head.dth);

				// For r thigh:
				slist.addValue(st.rthigh.x);
				slist.addValue(st.rthigh.y);
				slist.addValue(st.rthigh.th);
				slist.addValue(st.rthigh.dx);
				slist.addValue(st.rthigh.dy);
				slist.addValue(st.rthigh.dth);

				// For l thigh:
				slist.addValue(st.lthigh.x);
				slist.addValue(st.lthigh.y);
				slist.addValue(st.lthigh.th);
				slist.addValue(st.lthigh.dx);
				slist.addValue(st.lthigh.dy);
				slist.addValue(st.lthigh.dth);

				// For r calf:
				slist.addValue(st.rcalf.x);
				slist.addValue(st.rcalf.y);
				slist.addValue(st.rcalf.th);
				slist.addValue(st.rcalf.dx);
				slist.addValue(st.rcalf.dy);
				slist.addValue(st.rcalf.dth);

				// For l calf:
				slist.addValue(st.lcalf.x);
				slist.addValue(st.lcalf.y);
				slist.addValue(st.lcalf.th);
				slist.addValue(st.lcalf.dx);
				slist.addValue(st.lcalf.dy);
				slist.addValue(st.lcalf.dth);

				// For r foot:
				slist.addValue(st.rfoot.x);
				slist.addValue(st.rfoot.y);
				slist.addValue(st.rfoot.th);
				slist.addValue(st.rfoot.dx);
				slist.addValue(st.rfoot.dy);
				slist.addValue(st.rfoot.dth);

				// For l foot:
				slist.addValue(st.lfoot.x);
				slist.addValue(st.lfoot.y);
				slist.addValue(st.lfoot.th);
				slist.addValue(st.lfoot.dx);
				slist.addValue(st.lfoot.dy);
				slist.addValue(st.lfoot.dth);

				// For r upper arm:
				slist.addValue(st.ruarm.x);
				slist.addValue(st.ruarm.y);
				slist.addValue(st.ruarm.th);
				slist.addValue(st.ruarm.dx);
				slist.addValue(st.ruarm.dy);
				slist.addValue(st.ruarm.dth);

				// For L upper arm:
				slist.addValue(st.luarm.x);
				slist.addValue(st.luarm.y);
				slist.addValue(st.luarm.th);
				slist.addValue(st.luarm.dx);
				slist.addValue(st.luarm.dy);
				slist.addValue(st.luarm.dth);
				
				// For R lower arm:
				slist.addValue(st.rlarm.x);
				slist.addValue(st.rlarm.y);
				slist.addValue(st.rlarm.th);
				slist.addValue(st.rlarm.dx);
				slist.addValue(st.rlarm.dy);
				slist.addValue(st.rlarm.dth);

				// For L lower arm:
				slist.addValue(st.llarm.x);
				slist.addValue(st.llarm.y);
				slist.addValue(st.llarm.th);
				slist.addValue(st.llarm.dx);
				slist.addValue(st.llarm.dy);
				slist.addValue(st.llarm.dth);

				slist.build();
				Feature.Builder feat = Feature.newBuilder();
				
				slist.build();
				feats.addFeature(slist);
			}

			// Add all the actions for the dense run.
			for (Action act : dat.getAction()) {
				DenseData.Action.Builder action = DenseData.Action.newBuilder();
				action.setQ(act.peek()[0]);
				action.setW(act.peek()[1]);
				action.setO(act.peek()[2]);
				action.setP(act.peek()[3]);

				action.setActionTimesteps(act.getTimestepsTotal());
				action.build();
				data.addAction(action);
			}

			set.addDenseData(data.build());
			
		}
		FileOutputStream stream = new FileOutputStream(new File(fileName));
		set.build().writeTo(stream);
		stream.close();
	}
}
