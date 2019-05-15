package hardware;

import com.fazecast.jSerialComm.SerialPort;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

public class ArduinoSerial {

    private OutputStream out;

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
}