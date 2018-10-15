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
import tree.Utility;

public class Server {
    /**
     * Communication port. Router has 50000-51000 enabled
     **/
    public static final int port = 50000;

    /**
     * Still listening for the client to send States?
     **/
    public boolean active = true;

    /**
     * States and runs to be loaded from the TFRecord files. These will be inserted into whatever controller is sent in.
     **/
    private NavigableMap<Float, StateHolder> allStates;
    private Set<RunHolder> runs;

    /**
     * Controller sent in from the client. Allows the client to decide what settings to use.
     **/
    private Controller_NearestNeighborApprox receivedController;

    private ServerSocket serverSocket = null;
    private Socket socket = null;
    private ObjectOutputStream outStream;
    private ObjectInputStream inStream;


    private void loadFiles() {

        // CONTROLLER -- Get files loaded up.
        File saveLoc = new File(Utility.getExcutionPath() + "saved_data/training_data");

        File[] allFiles = saveLoc.listFiles();
        if (allFiles == null) throw new RuntimeException("Bad directory given: " + saveLoc.getName());

        List<File> exampleDataFiles = new ArrayList<>();
        for (File f : allFiles) {
            if (f.getName().contains("TFRecord")) {
                System.out.println("Found save file: " + f.getName());
                exampleDataFiles.add(f);
            }
        }
        Controller_NearestNeighborApprox controllerTemplate = new Controller_NearestNeighborApprox(exampleDataFiles);
        allStates = controllerTemplate.allStates;
        runs = controllerTemplate.runs;
    }

    /**
     * Open the IO.
     **/
    private void initialize() throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Waiting for connections on port " + port + ".");
        socket = serverSocket.accept();
        outStream = new ObjectOutputStream(socket.getOutputStream());
        inStream = new ObjectInputStream(socket.getInputStream());
    }

    /**
     * Wait for the client to send a controller along.
     **/
    private void awaitController() throws ClassNotFoundException, IOException {
        System.out.println("Waiting for a controller from the client.");
        receivedController = (Controller_NearestNeighborApprox) inStream.readObject();
        receivedController.runs = runs;
        receivedController.allStates = allStates;
        System.out.println("Received controller from client.");
    }

    /**
     * Wait for the client to send in states for the server to process.
     **/
    public void awaitStates() throws IOException, ClassNotFoundException {
        while (active) {
            State stateToProcess = (State) inStream.readObject();
            System.out.println("Received state to process from client.");
            receivedController.policy(stateToProcess);
            System.out.println("Trying to send full decision back.");
            outStream.writeObject(receivedController.getCurrentDecision().chosenAction);
            System.out.println("Sent state back to client.");
        }
        serverSocket.close();
        socket.close();
    }

    public static void main(String[] args) {

        Server server = new Server();
        server.loadFiles();
        server.launch();
    }

    private void launch() {
        final Runnable serverListen = new Thread() {
            @Override
            public void run() {
                try {
                    initialize();
                    awaitController();
                    awaitStates();
                } catch (ClassNotFoundException | IOException e) {
                    // IO exception go back and relaunch.
                    System.out.println("IO interrupted. Client probably disconnected. Waiting for a new controller.");
                    try {
                        socket.close();
                        serverSocket.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                    run();
                } finally {
                    try {
                        socket.close();
                        serverSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Future future = executor.submit(serverListen);
        executor.shutdown(); // This does not cancel the already-scheduled task.

        try {
            future.get(1, TimeUnit.HOURS);
        } catch (InterruptedException ie) {
            /* Handle the interruption. Or ignore it. */
            ie.printStackTrace();
        } catch (ExecutionException ee) {
            launch();
            ee.printStackTrace();
        } catch (TimeoutException te) {
            try {
                System.out.println("killing socket");
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            te.printStackTrace();
        }
        if (!executor.isTerminated())
            executor.shutdownNow(); // If you want to stop the code that hasn't finished.
    }
}