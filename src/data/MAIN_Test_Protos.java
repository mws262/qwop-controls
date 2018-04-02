package data;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import data.DenseDataProtos.DataSet.DenseData;
import game.State;
import data.DenseDataProtos.*;
import main.Action;

public class MAIN_Test_Protos {

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

		DataSet.Builder set = DataSet.newBuilder();
		
		for (SaveableDenseData dat : denseData) {
			DenseData.Builder data = DenseData.newBuilder();

			for (State st : dat.getState()) {
				DenseData.State.Builder state = DenseData.State.newBuilder();
				// Do for body:
				DenseData.State.StateVariable.Builder stateVar = DenseData.State.StateVariable.newBuilder();
				stateVar.setX(st.body.x);
				stateVar.setY(st.body.y);
				stateVar.setTh(st.body.th);
				stateVar.setDx(st.body.dx);
				stateVar.setDy(st.body.dy);
				stateVar.setDth(st.body.dth);
				stateVar.build();
				state.setBody(stateVar);

				// For head:
				stateVar = DenseData.State.StateVariable.newBuilder();
				stateVar.setX(st.head.x);
				stateVar.setY(st.head.y);
				stateVar.setTh(st.head.th);
				stateVar.setDx(st.head.dx);
				stateVar.setDy(st.head.dy);
				stateVar.setDth(st.head.dth);
				stateVar.build();
				state.setHead(stateVar);

				// For r thigh:
				stateVar = DenseData.State.StateVariable.newBuilder();
				stateVar.setX(st.rthigh.x);
				stateVar.setY(st.rthigh.y);
				stateVar.setTh(st.rthigh.th);
				stateVar.setDx(st.rthigh.dx);
				stateVar.setDy(st.rthigh.dy);
				stateVar.setDth(st.rthigh.dth);
				stateVar.build();
				state.setRthigh(stateVar);

				// For l thigh:
				stateVar = DenseData.State.StateVariable.newBuilder();
				stateVar.setX(st.lthigh.x);
				stateVar.setY(st.lthigh.y);
				stateVar.setTh(st.lthigh.th);
				stateVar.setDx(st.lthigh.dx);
				stateVar.setDy(st.lthigh.dy);
				stateVar.setDth(st.lthigh.dth);
				stateVar.build();
				state.setLthigh(stateVar);

				// For r calf:
				stateVar = DenseData.State.StateVariable.newBuilder();
				stateVar.setX(st.rcalf.x);
				stateVar.setY(st.rcalf.y);
				stateVar.setTh(st.rcalf.th);
				stateVar.setDx(st.rcalf.dx);
				stateVar.setDy(st.rcalf.dy);
				stateVar.setDth(st.rcalf.dth);
				stateVar.build();
				state.setRcalf(stateVar);

				// For l calf:
				stateVar = DenseData.State.StateVariable.newBuilder();
				stateVar.setX(st.lcalf.x);
				stateVar.setY(st.lcalf.y);
				stateVar.setTh(st.lcalf.th);
				stateVar.setDx(st.lcalf.dx);
				stateVar.setDy(st.lcalf.dy);
				stateVar.setDth(st.lcalf.dth);
				stateVar.build();
				state.setLcalf(stateVar);

				// For r foot:
				stateVar = DenseData.State.StateVariable.newBuilder();
				stateVar.setX(st.rfoot.x);
				stateVar.setY(st.rfoot.y);
				stateVar.setTh(st.rfoot.th);
				stateVar.setDx(st.rfoot.dx);
				stateVar.setDy(st.rfoot.dy);
				stateVar.setDth(st.rfoot.dth);
				stateVar.build();
				state.setRfoot(stateVar);

				// For l foot:
				stateVar = DenseData.State.StateVariable.newBuilder();
				stateVar.setX(st.lfoot.x);
				stateVar.setY(st.lfoot.y);
				stateVar.setTh(st.lfoot.th);
				stateVar.setDx(st.lfoot.dx);
				stateVar.setDy(st.lfoot.dy);
				stateVar.setDth(st.lfoot.dth);
				stateVar.build();
				state.setLfoot(stateVar);

				// For r upper arm:
				stateVar = DenseData.State.StateVariable.newBuilder();
				stateVar.setX(st.ruarm.x);
				stateVar.setY(st.ruarm.y);
				stateVar.setTh(st.ruarm.th);
				stateVar.setDx(st.ruarm.dx);
				stateVar.setDy(st.ruarm.dy);
				stateVar.setDth(st.ruarm.dth);
				stateVar.build();
				state.setRuarm(stateVar);

				// For L upper arm:
				stateVar = DenseData.State.StateVariable.newBuilder();
				stateVar.setX(st.luarm.x);
				stateVar.setY(st.luarm.y);
				stateVar.setTh(st.luarm.th);
				stateVar.setDx(st.luarm.dx);
				stateVar.setDy(st.luarm.dy);
				stateVar.setDth(st.luarm.dth);
				stateVar.build();
				state.setLuarm(stateVar);

				// For R lower arm:
				stateVar = DenseData.State.StateVariable.newBuilder();
				stateVar.setX(st.rlarm.x);
				stateVar.setY(st.rlarm.y);
				stateVar.setTh(st.rlarm.th);
				stateVar.setDx(st.rlarm.dx);
				stateVar.setDy(st.rlarm.dy);
				stateVar.setDth(st.rlarm.dth);
				stateVar.build();
				state.setRlarm(stateVar);

				// For L lower arm:
				stateVar = DenseData.State.StateVariable.newBuilder();
				stateVar.setX(st.llarm.x);
				stateVar.setY(st.llarm.y);
				stateVar.setTh(st.llarm.th);
				stateVar.setDx(st.llarm.dx);
				stateVar.setDy(st.llarm.dy);
				stateVar.setDth(st.llarm.dth);
				stateVar.build();
				state.setLlarm(stateVar);	

				// Finish this state:
				state.build();
				data.addState(state);
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
