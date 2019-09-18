package hardware;

import com.fazecast.jSerialComm.SerialPort;
import game.IGameCommandTarget;
import game.qwop.CommandQWOP;
import game.qwop.GameQWOP;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Handles the serial communication to the microcontroller that manages the key-pusher. Usually this will be the
 * Arduino Due.
 *
 * @author matt
 */
public class KeypusherSerialConnection implements IGameCommandTarget<CommandQWOP> {

    private OutputStream out;

    private final static Logger logger = LogManager.getLogger(KeypusherSerialConnection.class);

    public KeypusherSerialConnection() {
        // Get the correct serial port.
        SerialPort arduinoPort = null;
        SerialPort[] serialPorts = SerialPort.getCommPorts();
        for (SerialPort sp : serialPorts) {
            logger.info("Found: " + sp.getSystemPortName() + ", " + sp.getDescriptivePortName() + ", " + sp.getPortDescription());
            if (sp.getPortDescription().contains("USB-Based Serial Port") || sp.getPortDescription().contains(
                    "Arduino")) {
                if (arduinoPort == null) {
                    arduinoPort = sp;
                } else {
                    logger.warn("Multiple USB-serial ports found. May have identified the wrong one.");
                }
            }
        }
        if (arduinoPort == null) {
            throw new NullPointerException("Unable to find or open the serial port to the hardware key-pusher.");
        }

        arduinoPort.openPort();
        arduinoPort.setBaudRate(5000000);
        out = arduinoPort.getOutputStream();
        command(CommandQWOP.NONE);
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void command(CommandQWOP command){
        boolean q = command.get()[0];
        boolean w = command.get()[1];
        boolean o = command.get()[2];
        boolean p = command.get()[3];

        // These numbers are due to the mapping of 0-8 digits to ASCII codes or something like that.
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
        try {
            out.write(keyCodeOut);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getNumberOfChoices() {
        return GameQWOP.ACTIONSPACE_SIZE;
    }
}