//package goals;
//
//import controllers.Controller_Constant;
//import controllers.IController;
//import data.SavableActionSequence;
//import data.SavableFileIO;
//import data.SavableSingleGame;
//import game.qwop.GameQWOP;
//import game.command.Action;
//import game.command.ActionQueue;
//import game.state.IState;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//import tree.Utility;
//import tree.node.NodeGameExplorable;
//import tree.node.NodeGameGraphics;
//import vision.ScreenCapture;
//
//import javax.swing.*;
//import java.awt.*;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.io.File;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//
///**
// * Playback runs or sections of runs saved in SavableSingleRun files.
// *
// * @author matt
// */
//
//public class MAIN_Controlled extends JFrame implements Runnable, ActionListener {
//
//    private GameQWOP game = new GameQWOP();
//
//    /**
//     * Controller to use. Defaults to Controller_Constant and should usually be reassigned.
//     */
//    private IController controller = new Controller_Constant();
//
//    /**
//     * Place to load any 'prefix' run data in the form of a SavableSingleGame
//     */
//    private File prefixSave = new File("src/main/resources/" +
//            "saved_data/11_2_18/single_run_2018-11-08_08-41-13.SavableSingleGame");
//
//    private List<NodeGameGraphics> leafNodes = new ArrayList<>();
//
//    private SavableFileIO<SavableActionSequence> actionSaver = new SavableFileIO<>();
//
//    private String savePath = "src/main/resources/saved_data/individual_expansions_todo/";
//
//    private ActionQueue actionQueue = new ActionQueue();
//
//    private Color backgroundColor = Color.DARK_GRAY;
//
//    /**
//     * Screen capture settings.
//     */
//    private ScreenCapture screenCap;
//    private boolean doScreenCapture = false;
//
//    private Panel mainViewPanel;
//
//    private static final Logger logger = LogManager.getLogger(MAIN_Controlled.class);
//
//    public static void main(String[] args) {
//        MAIN_Controlled mc = new MAIN_Controlled();
//        mc.setup();
////         CONTROLLER -- Neural net picks keys.
////        mc.controller = new Controller_ClassifyPerTimestep(
////                "inference.pb", "src/main/resources/tflow_models", "tfrecord_input/split", "softmax/Softmax");
//
//        // CONTROLLER -- Approximate nearest neighbor.
//        File saveLoc = new File("src/main/resources/saved_data/training_data");
//
//        File[] allFiles = saveLoc.listFiles();
//        if (allFiles == null) throw new RuntimeException("Bad directory given: " + saveLoc.getName());
//
//        List<File> exampleDataFiles = new ArrayList<>();
//        for (File f : allFiles) {
//            if (f.getName().contains("TFRecord") && !f.getName().contains("recovery")) {
//                logger.info("Found save file: " + f.getName());
//                //if (count < 20) {
//                exampleDataFiles.add(f);
//                //}
//
//            }
//        }
//        mc.controller = new Controller_NearestNeighborApprox(exampleDataFiles);
//        mc.doControlled();
//    }
//
//    public void setup() {
//        if (doScreenCapture)
//            screenCap = new ScreenCapture(new File(Utility.generateFileName("vid",
//                    "mp4")));
//        //game.mainRunnerStroke = new BasicStroke(5);
//        mainViewPanel = new Panel();
//        this.setLayout(new BorderLayout());
//        add(mainViewPanel, BorderLayout.CENTER);
//
//        Thread graphicsThread = new Thread(this);
//        graphicsThread.start(); // Makes it smoother by updating the graphics faster than the timestep updates.
//
//        JButton saveButton = new JButton();
//        saveButton.setText("Save game.command");
//        saveButton.addActionListener(this);
//        saveButton.setVisible(true);
//        saveButton.setPreferredSize(new Dimension(1000, 50));
//        add(saveButton, BorderLayout.PAGE_END);
//
//        //game.mainRunnerColor = Color.ORANGE;
//        mainViewPanel.setBackground(backgroundColor);
//        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        // Window height
//        int windowHeight = 1000;
//        // Window width
//        int windowWidth = 1920;
//        setPreferredSize(new Dimension(windowWidth, windowHeight));
//        pack();
//
//        setVisible(true);
//        repaint();
//
////        // Save a progress log before shutting down.
////        if (doScreenCapture) Runtime.getRuntime().addShutdownHook(new Thread(() -> {
////            try {
////                screenCap.finish();
////            } catch (Exception e) {
////                e.printStackTrace();
////            }
////        }));
//    }
//
//    public void doControlled() {
//
//        // Recreate prefix part of this tree.
//        SavableFileIO<SavableSingleGame> fileIO = new SavableFileIO<>();
//        NodeGameGraphics rootNode = new NodeGameGraphics(GameQWOP.getInitialState());
//        List<SavableSingleGame> glist = new ArrayList<>();
//        fileIO.loadObjectsToCollection(prefixSave, glist);
//        NodeGameExplorable.makeNodesFromRunInfo(glist, rootNode);
//        leafNodes.clear();
//        rootNode.getLeaves(leafNodes);
//        NodeGameGraphics endNode = leafNodes.get(0);
//
//        // Back up the tree in order to skip the end of the prefix.
//        /* Will do the loaded prefix (open loop) to this tree depth before letting the controller take over. */
//        int doPrefixToDepth = 10;
//        while (endNode.getTreeDepth() > doPrefixToDepth) {
//            endNode = endNode.getParent();
//        }
//        // Run prefix part.
//        if (endNode.getTreeDepth() > 0) {
//            List<Action> actionList = new ArrayList<>();
//            endNode.getSequence(actionList);
//            actionQueue.addSequence(actionList);
//        }
//
//        while (!actionQueue.isEmpty()) {
//            executeNextOnQueue();
//            //
//            //			if (actionQueue.peekNextAction() == null) { // Experimental -- makes the final command end
//            // early so it doesn't throw it over to the controller right at a transition.
//            //				actionQueue.pollCommand();
//            //			}
//            try {
//                Thread.sleep(5);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//        // Enter controller mode.
//        //noinspection InfiniteLoopStatement
//        while (true) {
//            long initTime = System.currentTimeMillis();
//            IState state = game.getCurrentState();
//            Action nextAction = controller.policy(new NodeGameExplorable(state));
//            actionQueue.addAction(nextAction);
//            while (!actionQueue.isEmpty()) {
////                game.applyBodyImpulse(rand.nextFloat() - 0.5f, rand.nextFloat() - 0.5f);
////                game.applyBodyTorque(-2f);
//                executeNextOnQueue();
//                try {
//                    Thread.sleep(Long.max(20 - (System.currentTimeMillis() - initTime), 0L));
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                break;
//            }
//        }
//    }
//
//    @Override
//    public void run() {
//        //noinspection InfiniteLoopStatement
//        while (true) {
//            repaint();
//            try {
//                Thread.sleep(40);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    int count = 0;
//
//    /**
//     * Pop the next command off the queue and execute one timestep.
//     **/
//    private void executeNextOnQueue() {
//        if (!actionQueue.isEmpty()) {
//            game.step(actionQueue.pollCommand());
//            try {
//                if (doScreenCapture && count++ > 20) screenCap.takeFrameFromContainer(mainViewPanel);
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    private class Panel extends JPanel {
//        @Override
//        public void paintComponent(Graphics g) {
//            //if (!game.isGameInitialized()) return;
//            super.paintComponent(g);
//            /* Runner coordinates to pixels. */
//            float runnerScaling = 25f;
//            int yOffsetPixels = 450; // Drawing offsets within the viewing panel (i.e. non-physical).
//            int xOffsetPixels = 675;
//            controller.draw(g, game, runnerScaling, xOffsetPixels - (int) (runnerScaling * 2.5f), yOffsetPixels); // Optionally, the controller may want to draw some stuff for debugging.
//            game.draw(g, runnerScaling, xOffsetPixels, yOffsetPixels);
//        }
//    }
//
//    @Override
//    public void actionPerformed(ActionEvent e) {
//
//        if (e.getActionCommand().equals("Save game.command")) {
//            Action[] acts = actionQueue.getActionsInCurrentRun();
//            List<Action> actsConsolidated = Action.consolidateActions(Arrays.asList(acts));
//
//            Action[] actsOut = new Action[actsConsolidated.size()];
//            SavableActionSequence actionSequence = new SavableActionSequence(actsConsolidated.toArray(actsOut));
//            List<SavableActionSequence> actionList = new ArrayList<>();
//            actionList.add(actionSequence);
//            File saveFile = new File(savePath + "actions_" + Utility.getTimestamp() +
//                    ".SavableActionSequence");
//            actionSaver.storeObjects(actionList, saveFile, false);
//        }
//    }
//}
