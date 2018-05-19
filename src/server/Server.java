package server;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import controllers.Controller_NearestNeighborApprox;
import controllers.Controller_NearestNeighborApprox.RunHolder;
import controllers.Controller_NearestNeighborApprox.StateHolder;
import game.State;
import main.Action;
import main.Utility;

public class Server {
	public static final int port = 50000;
	protected ServerSocket ss = null;
	
	boolean active = true;
	NavigableMap<Float, StateHolder> allStates;
	Set<RunHolder> runs;
	
	Controller_NearestNeighborApprox receivedController;
	
	public void runServer() throws IOException, ClassNotFoundException {
		Socket socket = null;
		ss = new ServerSocket(port);
		System.out.println("Waiting for connections on port " + port);
		socket = ss.accept();
		ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
		ObjectInputStream is = new ObjectInputStream(socket.getInputStream());

		receivedController = (Controller_NearestNeighborApprox) is.readObject();
		receivedController.runs = runs;
		receivedController.allStates = allStates;
		System.out.println("received controller from client");
		
		while(active) {
			State stateToProcess = (State)is.readObject();
			System.out.println("Received state to process from client.");
			Action actToSend = receivedController.policy(stateToProcess);
			System.out.println("Sending state back to client.");
			os.writeObject(actToSend);
		}
		
		//os.writeObject(m);
		
		ss.close();
		socket.close();
	}

	public static void main(String[] args) throws ClassNotFoundException, IOException {
		
		Server s = new Server();
		
		// CONTROLLER -- Get files loaded up.
		File saveLoc = new File(Utility.getExcutionPath() + "saved_data/training_data");

		File[] allFiles = saveLoc.listFiles();
		if (allFiles == null) throw new RuntimeException("Bad directory given: " + saveLoc.getName());

		List<File> exampleDataFiles = new ArrayList<File>();
		for (File f : allFiles){
			if (f.getName().contains("TFRecord")) {
				System.out.println("Found save file: " + f.getName());
				exampleDataFiles.add(f);
			}
		}
		Controller_NearestNeighborApprox controllerTemplate = new Controller_NearestNeighborApprox(exampleDataFiles);
		s.runs = controllerTemplate.runs;
		s.allStates = controllerTemplate.allStates;
		
		final Runnable listen = new Thread() {
			@Override 
			public void run() { 
				try {
				s.runServer();
				} catch (ClassNotFoundException | IOException e) {
					e.printStackTrace();
				}finally {
					try {
						s.ss.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		};

		final ExecutorService executor = Executors.newSingleThreadExecutor();
		final Future future = executor.submit(listen);
		executor.shutdown(); // This does not cancel the already-scheduled task.

		try { 
			future.get(1, TimeUnit.HOURS); 
		}
		catch (InterruptedException ie) { 
			/* Handle the interruption. Or ignore it. */ 
			ie.printStackTrace();
		}
		catch (ExecutionException ee) { 
			/* Handle the error. Or ignore it. */ 
			ee.printStackTrace();
		}
		catch (TimeoutException te) { 
				try {
					System.out.println("killing socket");
					s.ss.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			te.printStackTrace();
		}
		if (!executor.isTerminated())
			executor.shutdownNow(); // If you want to stop the code that hasn't finished.
	}
}