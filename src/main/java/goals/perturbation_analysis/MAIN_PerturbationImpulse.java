package goals.perturbation_analysis;

import actions.ActionQueue;
import data.SavableFileIO;
import data.SavableSingleGame;
import game.GameLoader;
import actions.Action;
import tree.Node;
import ui.PanelRunner_MultiState;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class MAIN_PerturbationImpulse extends JFrame {

    public static void main(String[] args) {
        MAIN_PerturbationImpulse viewPerturbations = new MAIN_PerturbationImpulse();
        viewPerturbations.run("src/main/resources/saved_data/11_2_18/single_run_2018-11-13_09-30-25.SavableSingleGame");

    }

    public void run(String fileName) {
        // Vis setup.
        PanelRunner_MultiState panelRunner = new PanelRunner_MultiState();
        panelRunner.activateTab();
        getContentPane().add(panelRunner);
        setPreferredSize(new Dimension(2000, 400));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setVisible(true);


        // Load a saved game.
        List<SavableSingleGame> gameList = new ArrayList<>();
        SavableFileIO<SavableSingleGame> fileIO = new SavableFileIO<>();
        fileIO.loadObjectsToCollection(new File(fileName), gameList);

        assert !gameList.isEmpty();

        Action[] baseActions = gameList.get(0).actions;

        // Simulate the base actions.
        GameLoader game = new GameLoader();

        // These are the runners which will be perturbed.
        int numPerturbedRunners = 10;
        List<GameLoader> perturbedGames = new ArrayList<>();
        for (int i = 0; i < numPerturbedRunners; i++) {
            perturbedGames.add(new GameLoader());
        }


        ActionQueue actionQueue = new ActionQueue();
        actionQueue.addSequence(baseActions);

        int perturbationLocation = 15;
        // Get all runners to the perturbation location.
        while (actionQueue.getCurrentActionIdx() < perturbationLocation) {

            boolean[] command = actionQueue.pollCommand();
            game.stepGame(command);

            for (GameLoader perturbedGame : perturbedGames) {
                perturbedGame.stepGame(command);
            }
        }

        // Apply impulse disturbances.
        float impulseScaling = 2f;
        for (int i = 0; i < numPerturbedRunners; i++) {
            perturbedGames.get(i).applyBodyImpulse(impulseScaling  * (float)Math.cos((double)i/(double)numPerturbedRunners * 2. *
                    Math.PI), impulseScaling * (float)Math.sin((double)i/(double)numPerturbedRunners * 2. * Math.PI));
        }

        int drawInterval = 30;
        panelRunner.setMainState(game.getCurrentState());

        int count = 0;
        while (!actionQueue.isEmpty()) {
            boolean[] command = actionQueue.pollCommand();
            game.stepGame(command);
            if (count % drawInterval == 0)
                panelRunner.addSecondaryState(game.getCurrentState(), Color.BLACK);

            // Step perturbed runners.
            for (int i = 0; i < perturbedGames.size(); i++) {
                GameLoader thisGame = perturbedGames.get(i);
                thisGame.stepGame(command);
                if (count % drawInterval == 0)
                    panelRunner.addSecondaryState(perturbedGames.get(i).getCurrentState(), Node.getColorFromScaledValue(i
                        , numPerturbedRunners, 0.8f));

                // Remove and draw if failure.
                if (thisGame.getFailureStatus()) {
                    panelRunner.addSecondaryState(thisGame.getCurrentState(), Color.RED);
                    perturbedGames.remove(thisGame);
                }

            }

            panelRunner.repaint();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            count++;

        }



    }
}
