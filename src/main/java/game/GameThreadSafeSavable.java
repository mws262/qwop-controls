package game;

import data.FSTProxySerializer;
import org.nustaq.serialization.FSTConfiguration;

import java.io.*;
import java.lang.reflect.Proxy;

public class GameThreadSafeSavable implements Serializable {

    private static FSTConfiguration conf;
    static {
        conf = FSTConfiguration.createDefaultConfiguration();
        conf.registerSerializer(Proxy.class, new FSTProxySerializer(),true);
        conf.setLastResortResolver(p -> p.startsWith("com.sun.proxy.$Proxy") ? Proxy.class: null);

    }

    public final GameClassLoader classLoader;
    public final byte[] fullState;

    private GameThreadSafeSavable(GameClassLoader classLoader, byte[] fullState) {
        this.classLoader = classLoader;
        this.fullState = fullState;
    }

    public static synchronized GameThreadSafeSavable getFullState(GameThreadSafe game) {
//        ByteArrayOutputStream bout = new ByteArrayOutputStream();
//        try (ObjectOutputStream objOps = new ObjectOutputStream(bout)) {
//            objOps.writeObject(this);
//            objOps.flush();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return bout.toByteArray();
////        conf.setClassLoader(this);
        conf.setClassLoader(game.classLoader);
        conf.setForceSerializable(true);
        return new GameThreadSafeSavable(game.classLoader, conf.asByteArray(game));
    }

    public static synchronized GameThreadSafe getRestoredCopy(GameThreadSafeSavable gameSave) {
//        GameClassLoader classLoader = new GameClassLoader();
//        GameThreadSafeSavable gameRestored = null;
//        try (ByteArrayInputStream bin = new ByteArrayInputStream(fullState);
//             GameInputStream objIs = new GameInputStream(bin, classLoader)) {
//            gameRestored = (GameThreadSafeSavable) objIs.readObject();
//        } catch (ClassNotFoundException | IOException e) {
//            e.printStackTrace();
//        }
//        gameRestored.classLoader = classLoader;
//        GameClassLoader gameClassLoader = new GameClassLoader();
        conf.setClassLoader(gameSave.classLoader);
        conf.setForceSerializable(true);
        GameThreadSafe gameRestored = (GameThreadSafe) conf.asObject(gameSave.fullState);
        gameRestored.classLoader = gameSave.classLoader;
        return gameRestored;
    }

    private static class GameInputStream extends ObjectInputStream {
        private final ClassLoader customLoader;

        public GameInputStream(InputStream stream, ClassLoader customLoader) throws IOException {
            super(stream);
            this.customLoader = customLoader;
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass objectStreamClass) throws IOException, ClassNotFoundException {
            Class<?> clazz = Class.forName(objectStreamClass.getName(), false, customLoader);
            if (clazz != null) {
                return clazz;
            } else {
                return super.resolveClass(objectStreamClass);
            }
        }

        @Override
        protected Class<?> resolveProxyClass(String[] interfaces) throws IOException, ClassNotFoundException {
            Class<?>[] interfaceClasses = new Class[interfaces.length];
            for (int i = 0; i < interfaces.length; i++) {
                interfaceClasses[i] = Class.forName(interfaces[i], false, customLoader);
            }
            try {
                return Proxy.getProxyClass(customLoader, interfaceClasses);
            } catch (IllegalArgumentException e) {
                return super.resolveProxyClass(interfaces);
            }
        }
    }
}
