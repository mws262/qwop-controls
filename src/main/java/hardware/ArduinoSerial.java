package hardware;

import com.fazecast.jSerialComm.SerialPort;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

public class ArduinoSerial {

    volatile boolean q = false;
    volatile boolean w = false;
    volatile boolean o = false;
    volatile boolean p = false;

    OutputStream out;
    public ArduinoSerial() throws IOException {
        // Get the correct serial port.
        SerialPort arduinoPort = null;
        SerialPort[] serialPorts = SerialPort.getCommPorts();
        for (SerialPort sp : serialPorts) {
            System.out.println("Found: " + sp.getSystemPortName() + ", " + sp.getDescriptivePortName() + ", " + sp.getPortDescription());
            if (sp.getPortDescription().contains("USB-Based Serial Port") || sp.getPortDescription().contains(
                    "Arduino")) {
                if (arduinoPort == null) {
                    arduinoPort = sp;
                } else {
                    System.out.println("Multiple USB-serial ports found. May have identified the wrong one.");
                }
            }
        }
        Objects.requireNonNull(arduinoPort);

        arduinoPort.openPort();
        arduinoPort.setBaudRate(5000000);
        out = arduinoPort.getOutputStream();
        doCommand(false, false, false, false);
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void doCommand(boolean q, boolean w, boolean o, boolean p) throws IOException {
        int keyCodeOut = 48;
        if (q) {
            if (o) {
                keyCodeOut = 53;
            } else if (p) {
                keyCodeOut = 54;
            } else {
                keyCodeOut = 49;
            }
        } else if (w) {
            if (o) {
                keyCodeOut = 55;
            } else if (p) {
                keyCodeOut = 56;
            } else {
                keyCodeOut = 50;
            }

        } else if (o) {
            keyCodeOut = 51;
        } else if (p) {
            keyCodeOut = 52;
        }
        out.write(keyCodeOut);
        out.flush();
    }
//
//    private class Listener implements KeyListener {
//
//
//        @Override
//        public void keyTyped(KeyEvent e) {
//
//        }
//
//        @Override
//        public void keyPressed(KeyEvent e) {
//            switch (e.getKeyCode()) {
//                case KeyEvent.VK_Q:
//                    q = true;
//                    System.out.println("q");
//                    break;
//                case KeyEvent.VK_W:
//                    w = true;
//                    break;
//                case KeyEvent.VK_O:
//                    o = true;
//                    break;
//                case KeyEvent.VK_P:
//                    p = true;
//                    break;
//            }
//        }
//
//        @Override
//        public void keyReleased(KeyEvent e) {
//            switch (e.getKeyCode()) {
//                case KeyEvent.VK_Q:
//                    q = false;
//                    break;
//                case KeyEvent.VK_W:
//                    w = false;
//                    break;
//                case KeyEvent.VK_O:
//                    o = false;
//                    break;
//                case KeyEvent.VK_P:
//                    p = false;
//                    break;
//            }
//        }
//    }
//
//    public static void main(String[] args) throws InterruptedException, IOException {
//        new MAIN_ArduinoSerialTest();
//    }
}