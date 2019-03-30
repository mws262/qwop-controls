package flashqwop;
import org.json.JSONObject;

import java.io.*;
import java.net.*;


public class FlashQWOPServer {

    private PrintWriter dataOutput;

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

    public void sendCommand(boolean[] command) {
        String commandString =
                (command[0] ? "1" : "0") +
                        (command[1] ? "1" : "0") +
                        (command[2] ? "1" : "0") +
                        (command[3] ? "1" : "0") +
                        "0\0";
        dataOutput.print(commandString);
        dataOutput.flush();
    }

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

    public class DataReceiver implements Runnable {

        private InputStream inStream;
        private BufferedReader reader;


        public DataReceiver(InputStream inStream) {
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
                            System.out.println(msg);
                            JSONObject stateFromFlash = new JSONObject(msg);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}