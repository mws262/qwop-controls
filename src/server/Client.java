package server;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import controllers.Controller_NearestNeighborApprox;
import game.GameLoader;
import game.State;
import main.Action;
import main.IController;

public class Client {
	public static void main(String[] args) throws UnknownHostException, IOException, ClassNotFoundException {
		System.out.println("welcome client");
		Socket socket = new Socket("mattlinux.ddns.net", 50000);
		System.out.println("Client connected");
		ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
		System.out.println("Ok");
		Controller_NearestNeighborApprox cont = new Controller_NearestNeighborApprox(new ArrayList<File>());
		os.writeObject(cont);
		System.out.println("Sent controller template");

		GameLoader game = new GameLoader();
		State st = GameLoader.getInitialState();

		os.writeObject(st);


		for (int i =0; i < 100; i++) {
			ObjectInputStream is = new ObjectInputStream(socket.getInputStream());
			Action returnMessage = (Action) is.readObject();
			System.out.println("return Message is=" + returnMessage.toString());
		}
		socket.close();
	}
}