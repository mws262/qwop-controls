package flashqwop;

import game.action.Action;
import game.GameConstants;
import game.IGameExternal;
import game.state.State;
import game.state.StateVariable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import ui.runner.PanelRunner_SimpleState;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Server for communicating with the hacked Flash QWOP game. This server can send out commands to press any
 * combination of Q, W, O, and P keys as well as a command to reset the game. The server will receive 72-dim state
 * info from the game at every timestep (x, y, theta & velocities for 12 links). {@link IFlashStateListener} may add
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
public class FlashQWOPServer implements IGameExternal {

    private static URL qwopPageUrl;
    static {
        try {
            qwopPageUrl = new URL("http://localhost:8000/index.html");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

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
    private static final String
            none = "00000\0",
            q = "10000\0",
            w = "01000\0",
            o = "00100\0",
            p = "00010\0",
            qo = "10100\0",
            qp = "10010\0",
            wo = "01100\0",
            wp = "01010\0";

    private final boolean useJSONState = true;

    private static final Logger logger = LogManager.getLogger(FlashQWOPServer.class);
    /**
     * Open a socket for communicating back and forth with the real QWOP game.
     * @param port Specified port for communication. Currently real QWOP is hardcoded to use 2900.
     */
    public FlashQWOPServer(int port) {
        if (!doesServerExist())
            logger.error("QWOP local server does not exist. Run the shell script local_server_launch.sh; then reload " +
                    "localhost:8000/index.html");
        try {
            ServerSocket s = new ServerSocket(port); // TODO figure out how to make it start over if the client
            // disconnects.
            logger.info("Server started on port " + port + ". Waiting for connections....");
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
    @Override
    public void command(boolean[] command) {
        assert command.length == 4;
        command(command[0], command[1], command[2], command[3]);
    }

    /**
     * Send a command to the real QWOP game. It will stay active until another command overrides it. Hence, timestep
     * counting needs to be done elsewhere.
     * @param q Whether the q key is pressed (true is pressed).
     * @param w Whether the w key is pressed.
     * @param o Whether the o key is pressed.
     * @param p Whether the p key is pressed.
     */
    @Override
    public void command(boolean q, boolean w, boolean o, boolean p) {
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
        logger.debug("Sent game information request.");
    }

    /**
     * Send a signal to real QWOP to reset the game immediately.
     */
    public void sendResetSignal() {
        String commandString = "00001\0";
        dataOutput.println(commandString);
        dataOutput.flush();
        logger.debug("Sent reset request.");
    }

    /**
     * Just for minor testing/playing around to make sure the connections work properly.
     */
    public static void main(String[] args) {
        try {
            FlashQWOPServer server = new FlashQWOPServer(2900);

            // Waits for the EXIT command
            while (true) {
                server.command(new boolean[]{true, false, false, true});
                Thread.sleep(1000);
                server.command(new boolean[]{false, true, true, false});
                Thread.sleep(1000);
                server.sendResetSignal();
                Thread.sleep(1000);
                server.command(new boolean[]{false, false, false, false});
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            logger.warn("Connection lost");
        }
    }

    /**
     * Get the most-recently-received State from the Flash game. This is useful for one-off scenarios, but regular
     * consumers should add themselves as {@link IFlashStateListener} for immediate updates.
     * @return
     */
    @Override
    public State getCurrentState() {
        return dataInput.getCurrentState();
    }

    @Override
    public boolean getFailureStatus() {
        return dataInput.getFailureStatus();
    }

    @Override
    public long getTimestepsThisGame() {
        return dataInput.getCurrentTimestep();
    }

    @Override
    public int getStateDimension() {
        return 72;
    }

    /**
     * Any listeners will receive the updated state when it comes in from the real QWOP game.
     * @param listener A listener for the real QWOP state.
     */
    public void addStateListener(IFlashStateListener listener) {
        dataInput.addStateListener(listener);
    }


    public static boolean doesServerExist() {
        try {
            HttpURLConnection huc = (HttpURLConnection) qwopPageUrl.openConnection();
            huc.setRequestMethod("HEAD");
            huc.connect();
            int code = huc.getResponseCode();
            return code == 200;
        } catch(IOException e){
            return false;
        }
    }

    /**
     * Launch the web server and certificate server for the QWOP flash game. If the server is already running, then
     * this does nothing. TODO not quite worked out yet.
     */
    public static void launchWebserver() {
        if (doesServerExist())
            return;
        ProcessBuilder pb = new ProcessBuilder("sh", "./flash/local_server_launch.sh");
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT); // Makes sure that error messages and outputs go to console.
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        try {
            Process p = pb.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * State data input receiver for the Flash QWOP game. This should run on a separate thread to keep the state
     * updated in real time.
     */
    private class DataReceiver implements Runnable {

        /**
         * Reader which gets the input stream from the socket.
         */
        private BufferedReader reader;

        /**
         * Most recent time-step state received.
         */
        private AtomicInteger currentTimestep = new AtomicInteger();

        /**
         * Keep track of whether the game thinks it's failed.
         */
        private AtomicBoolean fallen = new AtomicBoolean(false);

        /**
         * Most recent state received.
         */
        private volatile State currentState;

        /**
         * List of listeners who will receive state updates as they come in.
         */
        private List<IFlashStateListener> listenerList = new ArrayList<>();

        /**
         * DataReceiver can draw the states coming in for debugging purposes. In general use, some listener should
         * handle this.
         */
        public boolean debugDraw = false;
        private PanelRunner_SimpleState panelRunner;

        @SuppressWarnings({"Duplicates", "ConstantConditions"})
        DataReceiver(InputStream inStream) {
            if (debugDraw) {
                panelRunner = new PanelRunner_SimpleState("Runner");
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
            reader = new BufferedReader(new InputStreamReader(inStream));
        }

        @Override
        public void run() {
            while (true) {
                try {
                    State st = null;
                    if (useJSONState) { // JSON state corresponds to socket.swf
                        if (reader.ready()) {
                            //long initialTime = System.currentTimeMillis();
                            String msg = reader.readLine().replace("\u0000", ""); // JSON parser hates the null character
                            // in front.
                            if (msg.contains("{")) {
                                JSONObject stateFromFlash = new JSONObject(msg);
                                if (stateFromFlash.keySet().contains("timestep")) {
                                    st = convertJSONToState(stateFromFlash);
                                    currentState = st;
                                    if (debugDraw) {
                                        panelRunner.updateState(st);
                                    }
                                    // Send the update to any listeners.
                                    for (IFlashStateListener listener : listenerList) {
                                        listener.stateReceived(getCurrentTimestep(), st);
                                    }
                                } else {
                                    logger.info(stateFromFlash.toString(2));
                                }
                            } else {
                                if (getCurrentTimestep() == 0) {
                                    logger.warn("From Flash game: " + msg);
                                }
                            }
                            //logger.debug(System.currentTimeMillis() - initialTime + "ms.");

                        }
                    } else { // Non-JSON is just a string of numbers that you need to know the order of to use. Set
                        // flag to false here, and use socket_numberstate.swf
                        if (reader.ready()) {
                            //long initialTime = System.currentTimeMillis();
                            String msg = reader.readLine();
                            msg = msg.replace("\u0000", ""); // JSON parser hates the null character

                            if (msg.contains(",")) {
                                st = convertStringToState(msg);
                                currentState = st;
                                if (debugDraw) {
                                    panelRunner.updateState(st);
                                }
                                // Send the update to any listeners.
                                for (IFlashStateListener listener : listenerList) {
                                    listener.stateReceived(getCurrentTimestep(), st);
                                }
                            } else {
                                logger.warn("From Flash game: " + msg);
                            }
                            //logger.debug(System.currentTimeMillis() - initialTime + "ms.");
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private State convertStringToState(String stateString) {
            String[] strArray = stateString.split(",");
            currentTimestep.set(Integer.parseInt(strArray[0]));
            boolean isFallen = Boolean.parseBoolean(strArray[1]);
            fallen.set(isFallen);

            float[] stateVals = new float[72];
            for (int i = 0; i < stateVals.length; i++) {
                stateVals[i] = Float.parseFloat(strArray[i + 2]);
            }
            stateVals[2] += GameConstants.torsoAngAdj;
            stateVals[8] += GameConstants.headAngAdj;
            stateVals[14] += GameConstants.rThighAngAdj;
            stateVals[20] += GameConstants.lThighAngAdj;
            stateVals[26] += GameConstants.rCalfAngAdj;
            stateVals[32] += GameConstants.lCalfAngAdj;

            stateVals[50] += GameConstants.rUArmAngAdj;
            stateVals[56] += GameConstants.lUArmAngAdj;
            stateVals[62] += GameConstants.rLArmAngAdj;
            stateVals[68] += GameConstants.lLArmAngAdj;
            return new State(stateVals, isFallen);
        }

        /**
         * JSON formatted data will come in from the real QWOP game. This will extract it, add in the angle offsets,
         * and return a state
         * @param stateFromFlash The JSON structure coming in over the XMLSocket.
         * @return A State compatible with all others throughout this project.
         */
        private State convertJSONToState(JSONObject stateFromFlash) {

            currentTimestep.set(stateFromFlash.getInt("timestep"));
            boolean isFallen = stateFromFlash.getBoolean("fallen");
            fallen.set(isFallen);

            JSONObject bodyMap = ((JSONObject)stateFromFlash.get("torso"));
            StateVariable torso = new StateVariable(bodyMap.getFloat("x"), bodyMap.getFloat("y"),
                    bodyMap.getFloat("th") + GameConstants.torsoAngAdj, bodyMap.getFloat("dx"), bodyMap.getFloat("dy"),
                    bodyMap.getFloat("dth"));

            bodyMap = ((JSONObject)stateFromFlash.get("head"));
            StateVariable head = new StateVariable(bodyMap.getFloat("x"), bodyMap.getFloat("y"),
                    bodyMap.getFloat("th") + GameConstants.headAngAdj, bodyMap.getFloat("dx"), bodyMap.getFloat("dy"),
                    bodyMap.getFloat("dth"));

            bodyMap = ((JSONObject)stateFromFlash.get("rthigh"));
            StateVariable rthigh = new StateVariable(bodyMap.getFloat("x"), bodyMap.getFloat("y"),
                    bodyMap.getFloat("th") + GameConstants.rThighAngAdj, bodyMap.getFloat("dx"), bodyMap.getFloat("dy"),
                    bodyMap.getFloat("dth"));

            bodyMap = ((JSONObject)stateFromFlash.get("lthigh"));
            StateVariable lthigh = new StateVariable(bodyMap.getFloat("x"), bodyMap.getFloat("y"),
                    bodyMap.getFloat("th") + GameConstants.lThighAngAdj, bodyMap.getFloat("dx"), bodyMap.getFloat("dy"),
                    bodyMap.getFloat("dth"));

            bodyMap = ((JSONObject)stateFromFlash.get("rcalf"));
            StateVariable rcalf = new StateVariable(bodyMap.getFloat("x"), bodyMap.getFloat("y"),
                    bodyMap.getFloat("th") + GameConstants.rCalfAngAdj, bodyMap.getFloat("dx"), bodyMap.getFloat("dy"),
                    bodyMap.getFloat("dth"));

            bodyMap = ((JSONObject)stateFromFlash.get("lcalf"));
            StateVariable lcalf = new StateVariable(bodyMap.getFloat("x"), bodyMap.getFloat("y"),
                    bodyMap.getFloat("th") + GameConstants.lCalfAngAdj, bodyMap.getFloat("dx"), bodyMap.getFloat("dy"),
                    bodyMap.getFloat("dth"));

            bodyMap = ((JSONObject)stateFromFlash.get("rfoot"));
            StateVariable rfoot = new StateVariable(bodyMap.getFloat("x"), bodyMap.getFloat("y"),
                    bodyMap.getFloat("th"), bodyMap.getFloat("dx"), bodyMap.getFloat("dy"), // No angle adjustment?
                    bodyMap.getFloat("dth"));

            bodyMap = ((JSONObject)stateFromFlash.get("lfoot"));
            StateVariable lfoot = new StateVariable(bodyMap.getFloat("x"), bodyMap.getFloat("y"),
                    bodyMap.getFloat("th"), bodyMap.getFloat("dx"), bodyMap.getFloat("dy"),
                    bodyMap.getFloat("dth"));

            bodyMap = ((JSONObject)stateFromFlash.get("ruarm"));
            StateVariable ruarm = new StateVariable(bodyMap.getFloat("x"), bodyMap.getFloat("y"),
                    bodyMap.getFloat("th") + GameConstants.rUArmAngAdj, bodyMap.getFloat("dx"), bodyMap.getFloat("dy"),
                    bodyMap.getFloat("dth"));

            bodyMap = ((JSONObject)stateFromFlash.get("luarm"));
            StateVariable luarm = new StateVariable(bodyMap.getFloat("x"), bodyMap.getFloat("y"),
                    bodyMap.getFloat("th") + GameConstants.lUArmAngAdj, bodyMap.getFloat("dx"), bodyMap.getFloat("dy"),
                    bodyMap.getFloat("dth"));

            bodyMap = ((JSONObject)stateFromFlash.get("rlarm"));
            StateVariable rlarm = new StateVariable(bodyMap.getFloat("x"), bodyMap.getFloat("y"),
                    bodyMap.getFloat("th") + GameConstants.rLArmAngAdj, bodyMap.getFloat("dx"), bodyMap.getFloat("dy"),
                    bodyMap.getFloat("dth"));

            bodyMap = ((JSONObject)stateFromFlash.get("llarm"));
            StateVariable llarm = new StateVariable(bodyMap.getFloat("x"), bodyMap.getFloat("y"),
                    bodyMap.getFloat("th") + GameConstants.lLArmAngAdj, bodyMap.getFloat("dx"), bodyMap.getFloat("dy"),
                    bodyMap.getFloat("dth"));

            return new State(torso, head, rthigh, lthigh, rcalf, lcalf, rfoot, lfoot, ruarm, luarm, rlarm, llarm,
                    isFallen);
        }

        /**
         * Get the most recent timestep number received from real QWOP. The first will be zero, and will be before
         * any physics stepping has occurred.
         * @return Most recent timestep received from the game.
         */
        public int getCurrentTimestep() {
            return currentTimestep.get();
        }

        /**
         * Get whether the most-recently obtained state is failed, according to the Flash game.
         * @return
         */
        public boolean getFailureStatus() {
            return fallen.get();
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
        public void addStateListener(IFlashStateListener listener) {
            listenerList.add(listener);
        }
    }
}