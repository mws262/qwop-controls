package flashqwop;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import game.GameConstants;
import game.GameUnified;
import game.State;
import game.StateVariable;
import tree.Utility;
import ui.PanelRunner_SimpleState;
import actions.Action;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Server for communicating with the hacked Flash QWOP game. This server can send out commands to press any
 * combination of Q, W, O, and P keys as well as a command to reset the game. The server will receive 72-dim state
 * info from the game at every timestep (x, y, theta & velocities for 12 links). {@link QWOPStateListener} may add
 * themselves to receive immediate updates as new state information comes in.
 *
 * The protocol for startup:
 * 1. In a terminal, cd to the directory containing crossdomain.xml.
 * 2. Launch the policy server and the http server: sudo sh local_server_launch.sh
 * 3. Run the Java code which will be interacting with the game.
 * 4. Launch the Flash game in one of these ways:
 *    a) Launch as new Chrome window: sh load_site.sh
 *    b) Navigate to localhost:8000/index.html
 *    c) Refresh the game if it's already open.
 *
 * @author matt
 */
public class FlashQWOPServer {

    /**
     * Writer which streams to the socket output. The Flash game should be listening to whatever this sends
     */
    private PrintWriter dataOutput;

    /**
     * Receives state info from the Flash QWOP game on its own thread.
     */
    private DataReceiver dataInput;

    /**
     * Cods for each action to send to the Flash game over the socket.
     */
    private static final String none = "00000\0";
    private static final String q = "10000\0";
    private static final String w = "01000\0";
    private static final String o = "00100\0";
    private static final String p = "00010\0";
    private static final String qo = "10100\0";
    private static final String qp = "10010\0";
    private static final String wo = "01100\0";
    private static final String wp = "01010\0";

    /**
     * Open a socket for communicating back and forth with the real QWOP game.
     * @param port Specified port for communication. Currently real QWOP is hardcoded to use 2900.
     */
    public FlashQWOPServer(int port) {
        try {
            ServerSocket s = new ServerSocket(port); // TODO figure out how to make it start over if the client
            // disconnects.
            System.out.println("Server started. Waiting for connections...");
            Socket incoming = s.accept();
            dataOutput = new PrintWriter(incoming.getOutputStream());

            // Start the receiver thread.
            dataInput = new DataReceiver(incoming.getInputStream());
            Thread listenerThread = new Thread(dataInput);
            listenerThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Open a socket for communicating back and forth with the real QWOP game. This will use the default port for
     * communication (2900).
     */
    public FlashQWOPServer() {
        this(2900);
    }

    /**
     * Send a command to the real QWOP game. It will stay active until another command overrides it. Hence, timestep
     * counting needs to be done elsewhere.
     * @param command 4 element Q, W, O, P command, with trues corresponding to pressed keys.
     */
    public void sendCommand(boolean[] command) {
        assert command.length == 4;
        sendCommand(command[0], command[1], command[2], command[3]);
    }

    /**
     * Send a command to the real QWOP game. It will stay active until another command overrides it. Hence, timestep
     * counting needs to be done elsewhere.
     * @param q Whether the q key is pressed (true is pressed).
     * @param w Whether the w key is pressed.
     * @param o Whether the o key is pressed.
     * @param p Whether the p key is pressed.
     */
    public void sendCommand(boolean q, boolean w, boolean o, boolean p) {
        // Trying to avoid building new strings all the time.
        Action.Keys keys = Action.booleansToKeys(q, w, o, p);
        String commandString;
        switch (keys) {
            case q:
                commandString = FlashQWOPServer.q;
                break;
            case w:
                commandString = FlashQWOPServer.w;
                break;
            case o:
                commandString = FlashQWOPServer.o;
                break;
            case p:
                commandString = FlashQWOPServer.p;
                break;
            case qp:
                commandString = FlashQWOPServer.qp;
                break;
            case wo:
                commandString = FlashQWOPServer.wo;
                break;
            case qo:
                commandString = FlashQWOPServer.qo;
                break;
            case wp:
                commandString = FlashQWOPServer.wp;
                break;
            case none:
                commandString = FlashQWOPServer.none;
                break;
            default:
                commandString = "\0";
        }
        dataOutput.print(commandString);
        dataOutput.flush();
    }

    public void sendInfoRequest() {
        String commandString = "getinfo";
        dataOutput.print(commandString);
        dataOutput.flush();
    }

    /**
     * Send a signal to real QWOP to reset the game immediately.
     */
    public void sendResetSignal() {
        String commandString = "00001\0";
        dataOutput.println(commandString);
        dataOutput.flush();
    }
    public static void main(String[] args) {
        try {
            FlashQWOPServer server = new FlashQWOPServer(2900);

            // Waits for the EXIT command
            while (true) {
                server.sendCommand(new boolean[]{true, false, false, true});
                Thread.sleep(1000);
                server.sendCommand(new boolean[]{false, true, true, false});
                Thread.sleep(1000);
                server.sendResetSignal();
                Thread.sleep(1000);
                server.sendCommand(new boolean[]{false, false, false, false});
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            System.out.println("Connection lost");
        }
    }

    /**
     * Get the most-recently-received State from the Flash game. This is useful for one-off scenarios, but regular
     * consumers should add themselves as {@link QWOPStateListener} for immediate updates.
     * @return
     */
    public State getCurrentState() {
        return dataInput.getCurrentState();
    }

    /**
     * Get the most recent timestep number received from real QWOP. The first will be zero, and will be before
     * any physics stepping has occurred.
     * @return Most recent timestep received from the game.
     */
    public int getCurrentTimestep() {
        return dataInput.getCurrentTimestep();
    }

    /**
     * Any listeners will receive the updated state when it comes in from the real QWOP game.
     * @param listener A listener for the real QWOP state.
     */
    public void addStateListener(QWOPStateListener listener) {
        dataInput.addStateListener(listener);
    }

    /**
     * State data input receiver for the Flash QWOP game. This should run on a separate thread to keep the state
     * updated in real time.
     */
    private static class DataReceiver implements Runnable {

        private Gson gson = new Gson();
        /**
         * Reader which gets the input stream from the socket.
         */
        private JsonReader reader;

        /**
         * Most recent state received.
         */
        private volatile State currentState;

        /**
         * List of listeners who will receive state updates as they come in.
         */
        private List<QWOPStateListener> listenerList = new ArrayList<>();

        /**
         * DataReceiver can draw the states coming in for debugging purposes. In general use, some listener should
         * handle this.
         */
        public boolean debugDraw = false;
        private PanelRunner_SimpleState panelRunner;

        @SuppressWarnings({"Duplicates", "ConstantConditions"})
        DataReceiver(InputStream inStream) {
            if (debugDraw) {
                panelRunner = new PanelRunner_SimpleState();
                JFrame frame = new JFrame(); // New frame to hold and manage the QWOP JPanel.
                frame.add(panelRunner);
                frame.setPreferredSize(new Dimension(600, 400));
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.pack();
                frame.setVisible(true);
                Thread panelThread = new Thread(panelRunner);
                panelThread.start();
                panelRunner.activateTab();
            }

            reader = new JsonReader(new InputStreamReader(inStream));
            reader.setLenient(true); // Parser doesn't like the JSON coming from flash for some reason, this is the
            // fix in the meantime.
        }

        @Override
        public void run() {
            while (true) {
                try {
                    if (reader.hasNext()) {

                        if (reader.peek().toString().equals("BEGIN_OBJECT")) {
                            try {
                                currentState = gson.fromJson(reader, State.class);

                                if (currentState.getTimestep() == 0) {
                                    System.out.println("restart");
                                }
                            } catch (com.google.gson.JsonSyntaxException e) {
                                e.printStackTrace();
                            }


                            for (QWOPStateListener listener : listenerList) {
                                listener.stateReceived(getCurrentTimestep(), currentState);
                            }
                            if (debugDraw) {
                                panelRunner.updateState(currentState);
                            }

                        } else {
                            reader.skipValue();
                        }
                    }
                } catch(IOException e){
                    e.printStackTrace();
                }
            }
        }

        /**
         * Get the most recent timestep number received from real QWOP. The first will be zero, and will be before
         * any physics stepping has occurred.
         * @return Most recent timestep received from the game.
         */
        public int getCurrentTimestep() {
            return currentState.getTimestep();
        }

        /**
         * Get whether the most-recently obtained state is failed, according to the Flash game.
         * @return
         */
        public boolean getFailureStatus() {
            return currentState.isFailed();
        }

        /**
         * Get the most-recently received State. This is good for one-off situations, but the listener approach
         * should be preferred for immediate updates.
         * @return The most recent State received from the Flash game.
         */
        public State getCurrentState() {
            return currentState;
        }

        /**
         * Any listeners will receive the updated state when it comes in from the real QWOP game.
         * @param listener A listener for the real QWOP state.
         */
        public void addStateListener(QWOPStateListener listener) {
            listenerList.add(listener);
        }
    }
}