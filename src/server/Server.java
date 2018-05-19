package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static final int port = 4444;
    private ServerSocket ss = null;

    public void runServer() throws IOException, ClassNotFoundException{
        ss = new ServerSocket(port);
        System.out.println("le systeme est pret pour accepter les connexions");
        Socket socket = ss.accept();
        ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream is = new ObjectInputStream(socket.getInputStream());
        
        Message m = (Message) is.readObject();
        doSomething(m);

        os.writeObject(m);
        socket.close(); 
    }

    private void doSomething(Message m) {
        m.setResult(new Integer(m.getA().intValue()*m.getB().intValue()));
    }

    public static void main(String[] args) throws ClassNotFoundException, IOException {
        new Server().runServer();
    }
}