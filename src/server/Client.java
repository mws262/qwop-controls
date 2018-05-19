package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {
    public static void main(String[] args) throws UnknownHostException, IOException, ClassNotFoundException {
        System.out.println("welcome client");
        Socket socket = new Socket("localhost", 4444);
        System.out.println("Client connected");
        ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
        System.out.println("Ok");
        Message message = new Message(new Integer(15), new Integer(32));
        os.writeObject(message);
        System.out.println("Envoi des informations au serveur ...");

        ObjectInputStream is = new ObjectInputStream(socket.getInputStream());
        Message returnMessage = (Message) is.readObject();
        System.out.println("return Message is=" + returnMessage);
        socket.close();
    }
}