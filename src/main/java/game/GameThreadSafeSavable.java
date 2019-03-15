package game;

import org.nustaq.serialization.FSTConfiguration;

import java.io.*;

public class GameThreadSafeSavable implements Serializable {

    private static FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();

    private final GameClassLoader classLoader;
    private final byte[] fullState;

    private GameThreadSafeSavable(GameClassLoader classLoader, byte[] fullState) {
        this.classLoader = classLoader;
        this.fullState = fullState;
    }

    public static synchronized GameThreadSafeSavable getFullState(GameThreadSafe game) {
        conf.setClassLoader(game.classLoader);
        conf.setForceSerializable(true);
        return new GameThreadSafeSavable(game.classLoader, conf.asByteArray(game));
    }

    public static synchronized GameThreadSafe getRestoredCopy(GameThreadSafeSavable gameSave) {
        conf.setClassLoader(gameSave.classLoader);
        conf.setForceSerializable(true);
        GameThreadSafe gameRestored = (GameThreadSafe) conf.asObject(gameSave.fullState);
        return gameRestored;
    }
}
