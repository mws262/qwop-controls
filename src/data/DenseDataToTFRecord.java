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

		for (SaveableDenseData dat : denseData) {
			SequenceExample.Builder seqEx = SequenceExample.newBuilder();
			
			// Use timestamp as unique run identifier.
//			Int64List.Builder identifier = Int64List.newBuilder();
//			identifier.addValue(System.currentTimeMillis());
//			Feature.Builder identifierFeat = Feature.newBuilder(); // It gets its own feature.
//			identifierFeat.setInt64List(identifier.build());
//			seqEx.setContext(identifierFeat.build());
			
			
			FeatureLists.Builder featLists = FeatureLists.newBuilder(); // Featlists is a single run
			FeatureList.Builder featList = FeatureList.newBuilder(); //Single featlist is just states or just actions.
		
			for (State st : dat.getState()) {
				Feature.Builder feat = Feature.newBuilder();
				FloatList.Builder flist = FloatList.newBuilder();
				
				// Do for body:
				flist.addValue(st.body.x);
				flist.addValue(st.body.y);
				flist.addValue(st.body.th);
				flist.addValue(st.body.dx);
				flist.addValue(st.body.dy);
				flist.addValue(st.body.dth);

				// For head:
				flist.addValue(st.head.x);
				flist.addValue(st.head.y);
				flist.addValue(st.head.th);
				flist.addValue(st.head.dx);
				flist.addValue(st.head.dy);
				flist.addValue(st.head.dth);

				// For r thigh:
				flist.addValue(st.rthigh.x);
				flist.addValue(st.rthigh.y);
				flist.addValue(st.rthigh.th);
				flist.addValue(st.rthigh.dx);
				flist.addValue(st.rthigh.dy);
				flist.addValue(st.rthigh.dth);

				// For l thigh:
				flist.addValue(st.lthigh.x);
				flist.addValue(st.lthigh.y);
				flist.addValue(st.lthigh.th);
				flist.addValue(st.lthigh.dx);
				flist.addValue(st.lthigh.dy);
				flist.addValue(st.lthigh.dth);

				// For r calf:
				flist.addValue(st.rcalf.x);
				flist.addValue(st.rcalf.y);
				flist.addValue(st.rcalf.th);
				flist.addValue(st.rcalf.dx);
				flist.addValue(st.rcalf.dy);
				flist.addValue(st.rcalf.dth);

				// For l calf:
				flist.addValue(st.lcalf.x);
				flist.addValue(st.lcalf.y);
				flist.addValue(st.lcalf.th);
				flist.addValue(st.lcalf.dx);
				flist.addValue(st.lcalf.dy);
				flist.addValue(st.lcalf.dth);

				// For r foot:
				flist.addValue(st.rfoot.x);
				flist.addValue(st.rfoot.y);
				flist.addValue(st.rfoot.th);
				flist.addValue(st.rfoot.dx);
				flist.addValue(st.rfoot.dy);
				flist.addValue(st.rfoot.dth);

				// For l foot:
				flist.addValue(st.lfoot.x);
				flist.addValue(st.lfoot.y);
				flist.addValue(st.lfoot.th);
				flist.addValue(st.lfoot.dx);
				flist.addValue(st.lfoot.dy);
				flist.addValue(st.lfoot.dth);

				// For r upper arm:
				flist.addValue(st.ruarm.x);
				flist.addValue(st.ruarm.y);
				flist.addValue(st.ruarm.th);
				flist.addValue(st.ruarm.dx);
				flist.addValue(st.ruarm.dy);
				flist.addValue(st.ruarm.dth);

				// For L upper arm:
				flist.addValue(st.luarm.x);
				flist.addValue(st.luarm.y);
				flist.addValue(st.luarm.th);
				flist.addValue(st.luarm.dx);
				flist.addValue(st.luarm.dy);
				flist.addValue(st.luarm.dth);
				
				// For R lower arm:
				flist.addValue(st.rlarm.x);
				flist.addValue(st.rlarm.y);
				flist.addValue(st.rlarm.th);
				flist.addValue(st.rlarm.dx);
				flist.addValue(st.rlarm.dy);
				flist.addValue(st.rlarm.dth);

				// For L lower arm:
				flist.addValue(st.llarm.x);
				flist.addValue(st.llarm.y);
				flist.addValue(st.llarm.th);
				flist.addValue(st.llarm.dx);
				flist.addValue(st.llarm.dy);
				flist.addValue(st.llarm.dth);

				feat.setFloatList(flist.build());
				featList.addFeature(feat.build());
			}

			featLists.putFeatureList("states", featList.build());
			
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
