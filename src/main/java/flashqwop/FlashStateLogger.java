package flashqwop;

import game.state.IState;
import game.state.State;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tree.Utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class FlashStateLogger implements IFlashStateListener {
    File logFile;
    PrintWriter writer;

    Logger logger = LogManager.getLogger(FlashStateLogger.class);
    @Override
    public void stateReceived(int timestep, IState state) {
        if (state instanceof State) {
            State stateBasic = (State) state;
            if (timestep == 0) {
                logFile = new File("./src/main/resources/saved_data/" + Utility.generateFileName("flash_states", "txt"));
                logger.info("Made a new Flash QWOP state log at: " + logFile.getName());
                if (writer != null) {
                    writer.close();
                }
                try {
                    writer = new PrintWriter(logFile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            } else if (logFile == null) {
                return;
            }

            writer.println(stateBasic.toFlatString());

            if (state.isFailed()) {
                writer.close();
            }
        } else {
            logger.warn("Different state type than State was passed in. This is currently not supported, so no saving" +
                    " has occured.");
        }
    }
}
