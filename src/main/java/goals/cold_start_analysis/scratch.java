package goals.cold_start_analysis;

import data.SavableFileIO;
import game.GameSingleThread;
import game.State;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class Scratch {
    public static void main(String[] args) {

        GameSingleThread game = GameSingleThread.getInstance();
        SavableFileIO<GameSingleThread> gameSaver = new SavableFileIO<>();

        for (int i = 0; i < 10; i++) {
            game.step(false, true, true, false);
        }


        try (FileOutputStream fin = new FileOutputStream(new File("./test.game"), false); ObjectOutputStream objOps =
                new ObjectOutputStream(fin)) {
            objOps.writeObject(game);
            objOps.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < 10; i++) {
            game.step(false, true, true, false);
        }

        State state1 = game.getCurrentState();
        game.releaseGame();


        GameSingleThread gameLoaded = GameSingleThread.getInstance();
        try (FileInputStream fin = new FileInputStream(new File("./test.game")); ObjectInputStream objIs = new ObjectInputStream(fin)) {
            gameLoaded = (GameSingleThread) objIs.readObject();

        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < 10; i++) {
            gameLoaded.step(false, true, true, false);
        }

        State state2 = gameLoaded.getCurrentState();

    }
}