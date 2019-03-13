package game;

import java.io.*;
import java.lang.reflect.Proxy;

public class GameThreadSafeSavable extends GameThreadSafe implements Serializable {

    public synchronized byte[] getFullState() {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try (ObjectOutputStream objOps = new ObjectOutputStream(bout)) {
            objOps.writeObject(this);
            objOps.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bout.toByteArray();
    }

    public synchronized GameThreadSafeSavable getRestoredCopy(byte[] fullState) {
        GameThreadSafeSavable gameRestored = null;
        try (ByteArrayInputStream bin = new ByteArrayInputStream(fullState);
             GameInputStream objIs = new GameInputStream(bin, this)) {
            gameRestored = (GameThreadSafeSavable) objIs.readObject();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
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
