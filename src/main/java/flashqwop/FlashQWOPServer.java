package flashqwop;
import game.GameConstants;
import game.State;
import game.StateVariable;
import org.json.JSONObject;
import ui.PanelRunner_SimpleState;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;


public class FlashQWOPServer {

    private PrintWriter dataOutput;

    /**
     * Open a socket for communicating back and forth with the real QWOP game.
     * @param port Specified port for communication. Currently real QWOP is hardcoded to use 2900.
     */
    public FlashQWOPServer(int port) {
        try {
            ServerSocket s = new ServerSocket(port);
            System.out.println("Server started. Waiting for connections...");
            Socket incoming = s.accept();
            dataOutput = new PrintWriter(incoming.getOutputStream());

            Thread listenerThread = new Thread(new DataReceiver(incoming.getInputStream()));
            listenerThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        String commandString =
                (q ? "1" : "0") +
                        (w ? "1" : "0") +
                        (o ? "1" : "0") +
                        (p ? "1" : "0") +
                        "0\0";
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

    public static class DataReceiver implements Runnable {

        private InputStream inStream;
        private BufferedReader reader;
        private PanelRunner_SimpleState panelRunner;

        private AtomicInteger currentTimestep = new AtomicInteger();

        private List<QWOPStateListener> listenerList = new ArrayList<>();

        public DataReceiver(InputStream inStream) {
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
            this.inStream = inStream;
            reader = new BufferedReader(new InputStreamReader(inStream));
        }

        @Override
        public void run() {
            while (true) {
                try {
                    if (reader.ready()) {
                        String msg = reader.readLine().replace("\u0000", ""); // JSON parser hates the null character
                        // in front.
                        if (msg.contains("{")) {
                            //System.out.println(msg);
                            JSONObject stateFromFlash = new JSONObject(msg);
                            convertJSONToState(stateFromFlash);
                        } // TODO handle other messages.
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * JSON formatted data will come in from the real QWOP game. This will extract it, add in the angle offsets,
         * and return a state
         * @param stateFromFlash The JSON structure coming in over the XMLSocket.
         * @return A State compatible with all others throughout this project.
         */
        private State convertJSONToState(JSONObject stateFromFlash) {

            currentTimestep.set(stateFromFlash.getInt("timestep"));

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

            State st = new State(torso, head, rthigh, lthigh, rcalf, lcalf, rfoot, lfoot, ruarm, luarm, rlarm, llarm,
                    false);
            panelRunner.updateState(st);
            // Send the update to any listeners.
            for (QWOPStateListener listener : listenerList) {
                listener.stateReceived(st);
            }
            return st;
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
         * Any listeners will receive the updated state when it comes in from the real QWOP game.
         * @param listener A listener for the real QWOP state.
         */
        public void addStateListener(QWOPStateListener listener) {
            listenerList.add(listener);
        }
    }
}