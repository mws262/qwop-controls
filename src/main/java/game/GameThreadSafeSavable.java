package game;

import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.serializers.FSTCollectionSerializer;

import java.io.*;
import java.util.HashSet;

public class GameThreadSafeSavable implements Serializable {

    private static FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();
    static {
        conf.registerSerializer(HashSet.class,new FSTCollectionSerializer(),true);
    }

    private final GameClassLoader classLoader;
    private final byte[] fullState;

    private GameThreadSafeSavable(GameClassLoader classLoader, byte[] fullState) {
        this.classLoader = classLoader;
        this.fullState = fullState;
    }

    public static synchronized GameThreadSafeSavable getFullState(GameThreadSafe game) {
//        conf.setClassLoader(game.classLoader);
        conf.setForceSerializable(true);
        conf.setForceClzInit(true);

        return new GameThreadSafeSavable(game.classLoader, conf.asByteArray(game));
    }

    public static synchronized GameThreadSafe getRestoredCopy(GameThreadSafeSavable gameSave) {
        conf.setClassLoader(gameSave.classLoader);
        conf.setForceSerializable(true);
        conf.setForceClzInit(true);
        return (GameThreadSafe) conf.asObject(gameSave.fullState);
    }
}
