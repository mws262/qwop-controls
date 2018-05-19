package server;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import controllers.Controller_NearestNeighborApprox;
import main.IController;

public class Client {
    public static void main(String[] args) throws UnknownHostException, IOException, ClassNotFoundException {
        System.out.println("welcome client");
        Socket socket = new Socket("mattlinux.ddns.net", 50000);
        System.out.println("Client connected");
        ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
        System.out.println("Ok");
        IController cont = new Controller_NearestNeighborApprox(new ArrayList<File>());
        os.writeObject(cont);
        System.out.println("Envoi des informations au serveur ...");

        ObjectInputStream is = new ObjectInputStream(socket.getInputStream());
        Message returnMessage = (Message) is.readObject();
        System.out.println("return Message is=" + returnMessage);
        socket.close();
    }
}