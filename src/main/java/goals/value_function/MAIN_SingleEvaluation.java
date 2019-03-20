package goals.value_function;

import actions.Action;
import actions.ActionGenerator_FixedActions;
import actions.ActionQueue;
import actions.ActionSet;
import distributions.Distribution_Equal;
import game.GameUnified;
import tree.Node;
import tree.Utility;
import value.ValueFunction_TensorFlow;
import value.ValueFunction_TensorFlow_StateOnly;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@SuppressWarnings("ALL")
public class MAIN_SingleEvaluation extends JPanel implements ActionListener {

    GameUnified game = new GameUnified();

    public static void main(String[] args) {
//        boolean doScreenCapture = false;
//        ScreenCapture screenCapture = new ScreenCapture(new File(Utility.generateFileName("vid","mp4")));
//        if (doScreenCapture) {
//            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//                try {
//                    screenCapture.finalize();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }));
//        }

        // Set up the visualizer.
        MAIN_SingleEvaluation qwop = new MAIN_SingleEvaluation();
        qwop.game.failOnThighContact = false;
        JFrame frame = new JFrame(); // New frame to hold and manage the QWOP JPanel.
        frame.add(qwop);
        frame.setPreferredSize(new Dimension(600, 400));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        // Fire game update every 40 ms.
        Timer timer = new Timer(40, qwop);
        timer.start();

        // Load a value function controller.
        ValueFunction_TensorFlow valueFunction = null;
        try {
            valueFunction = new ValueFunction_TensorFlow_StateOnly(new File("src/main/resources/tflow_models" +
                    "/state_only.pb"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        valueFunction.loadCheckpoint("chk2");

        // Assign potential actions for the value function to choose among.
        ActionSet actionSetNone = ActionSet.makeActionSet(IntStream.range(1, 30).toArray(), new boolean[]{false, false,
                false, false}, new Distribution_Equal()); // None, None
        ActionSet actionSetWO = ActionSet.makeActionSet(IntStream.range(1, 50).toArray(), new boolean[]{false, true,
                true, false}, new Distribution_Equal()); // W, O
        ActionSet actionSetQP = ActionSet.makeActionSet(IntStream.range(1, 50).toArray(), new boolean[]{true, false,
                false, true}, new Distribution_Equal()); // Q, P

        ActionSet allActions = new ActionSet(new Distribution_Equal());
        allActions.addAll(actionSetNone);
        allActions.addAll(actionSetWO);
        allActions.addAll(actionSetQP);

        ActionGenerator_FixedActions actionGenerator = new ActionGenerator_FixedActions(allActions);
        Node.potentialActionGenerator = actionGenerator;
//        assignAllowableActions(-1);
        Node rootNode = new Node();

        // Assign a "prefix" of actions, since I'm not sure if the controller will generalize to this part of running.
        List<Action[]> alist = new ArrayList<>();
        alist.add(new Action[]{
                new Action(1,false,false,false,false),
                new Action(34,false,true,true,false),
                new Action(19,false,false,false,false),
                new Action(45,true,false,false,true),

                new Action(10,false,false,false,false),
                new Action(27,false,true,true,false),
//                 new Action(8,false,false,false,false),
//                new Action(20,true,false,false,true),
        });

        Node.makeNodesFromActionSequences(alist, rootNode, new GameUnified());
        Node.stripUncheckedActionsExceptOnLeaves(rootNode, 7);

        List<Node> leaf = new ArrayList<>();
        rootNode.getLeaves(leaf);

        ActionQueue actionQueue = new ActionQueue();
        actionQueue.addSequence(leaf.get(0).getSequence());

        // Run the "prefix" section.
        while (!actionQueue.isEmpty()) {
            qwop.game.step(actionQueue.pollCommand());
//            if (doScreenCapture) {
//                try {
//                    screenCapture.takeFrameFromContainer(frame);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//            try {
//                Thread.sleep(10);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        }

        Node currNode = leaf.get(0);

        // Run the controller until failure.
        while (!qwop.game.getFailureStatus()) {

            Utility.tic();
            Action chosenAction = valueFunction.getMaximizingAction(currNode, qwop.game);
            Utility.toc();

            actionQueue.addAction(chosenAction);
            while (!actionQueue.isEmpty()) {
                long currTime = System.currentTimeMillis();
                // qwop.game.applyBodyImpulse(-3f, 0.001f);
                qwop.game.step(actionQueue.pollCommand());

//                if (doScreenCapture) {
//                    try {
//                        screenCapture.takeFrameFromContainer(frame);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                } else { // Screen capture is already so slow, we don't need a delay.
                    try {
                        Thread.sleep(Math.max(1, 40 - (System.currentTimeMillis() - currTime)));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
//                }
            }
            currNode = currNode.addChild(chosenAction);
            currNode.setState(qwop.game.getCurrentState());
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        game.draw(g, 10, 300, 200); // Redraws the game. Scaling and offsets are handpicked to work for the size of
        // the window.
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        repaint();
    }
}
