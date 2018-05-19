package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import main.IController;

public class Server {
	public static final int port = 50000;
	protected ServerSocket ss = null;

	public void runServer() throws IOException, ClassNotFoundException {
		Socket socket = null;
		ss = new ServerSocket(port);
		System.out.println("Waiting for connections on port " + port);
		socket = ss.accept();
		ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
		ObjectInputStream is = new ObjectInputStream(socket.getInputStream());

		IController m = (IController) is.readObject();

		os.writeObject(m);
		
		ss.close();
		socket.close();
	}

	public static void main(String[] args) throws ClassNotFoundException, IOException {

		Server s = new Server();
		
		final Runnable stuffToDo = new Thread() {
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
		final Future future = executor.submit(stuffToDo);
		executor.shutdown(); // This does not cancel the already-scheduled task.

		try { 
			future.get(30, TimeUnit.SECONDS); 
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