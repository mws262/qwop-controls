package goals.interactive;

import game.GameLearned;
import game.GameUnified;
import game.State;
import actions.Action;
import ui.PanelRunner;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.ArrayList;

/**
 * This is a playable QWOP game. Press Q, W, O, P keys to move. R resets the runner to its original state.
 * This is meant to be a standalone example of using the interface to the game.
 *
 * @author matt
 */
public class PlayAndTrain extends JPanel implements KeyListener, ActionListener {

    /**
     * Keep track of which keys are down. True means currently pressed.
     */
    private boolean q, w, o, p;

    /**
     * Game physics world to use.
     */
    private GameUnified game = new GameUnified();
    private GameLearned gameToTrain;

    List<State> statesInRun = new ArrayList<>();
    List<boolean[]> commandsInRun = new ArrayList<>();


    PlayAndTrain() {
        List<Integer> layers = new ArrayList<>();
        layers.add(32);
//        layers.add(64);
        List<String> opts = new ArrayList<>();
        opts.add("--learnrate");
        opts.add("0.00001");
        try {
            gameToTrain = new GameLearned("simulator_graph", layers, new ArrayList<>());
//            gameToTrain = new GameLearned(new File("src/main/resources/tflow_models/simulator_graph.pb"));
            //gameToTrain.loadCheckpoint("simchk");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        gameToTrain.giveAllStates(GameUnified.getInitialState(), GameUnified.getInitialState(),
                GameUnified.getInitialState(), Action.Keys.none, Action.Keys.none);
    }
    @Override
    public void actionPerformed(ActionEvent e) { // Gets called every 40ms
        boolean[] commands = new boolean[] {q, w, o, p};
        game.step(commands); // Step the game forward 1 timestep with the specified keys pressed.
        gameToTrain.step(commands);
        gameToTrain.updateStates(gameToTrain.getCurrentState());

        statesInRun.add(game.getCurrentState());
        commandsInRun.add(commands);

        repaint(); // Redraw the runner and scene.
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (game != null && gameToTrain != null) {
            game.draw(g, 10, 300, 200); // Redraws the game. Scaling and offsets are handpicked to work for the size of
            // the window.
            gameToTrain.draw(g, 10, 300, 200); // Redraws the game. Scaling and offsets are handpicked to work for the size of
            // the window.
        }

        PanelRunner.keyDrawer(g, q, w, o, p, -50, 20, 240, 40);
    }

    @Override
    public void keyPressed(KeyEvent event) {
        switch (event.getKeyCode()) {
            case KeyEvent.VK_Q:
                q = true;
                break;
            case KeyEvent.VK_W:
                w = true;
                break;
            case KeyEvent.VK_O:
                o = true;
                break;
            case KeyEvent.VK_P:
                p = true;
                break;
            case KeyEvent.VK_R: // Reset the runner on pressing r.
                gameToTrain.assembleWholeRunForTraining(statesInRun, commandsInRun);
                statesInRun.clear();
                commandsInRun.clear();
                game.makeNewWorld();
                statesInRun.add(GameUnified.getInitialState());
                gameToTrain.giveAllStates(GameUnified.getInitialState(), GameUnified.getInitialState(),
                        GameUnified.getInitialState(), Action.Keys.none, Action.Keys.none);
                break;
            default:
                // Nothing
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent event) {
        switch (event.getKeyCode()) {
            case KeyEvent.VK_Q:
                q = false;
                break;
            case KeyEvent.VK_W:
                w = false;
                break;
            case KeyEvent.VK_O:
                o = false;
                break;
            case KeyEvent.VK_P:
                p = false;
                break;
            default:
                // Nothing.
                break;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        // JPanel that runs and displays the game.
        PlayAndTrain qwop = new PlayAndTrain();

        JFrame frame = new JFrame(); // New frame to hold and manage the QWOP JPanel.
        frame.add(qwop);
        frame.addKeyListener(qwop); // Listen for user input.
        frame.setPreferredSize(new Dimension(600, 400));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        // Fire game update every 40 ms.
        new Timer(40, qwop).start();
    }
}
