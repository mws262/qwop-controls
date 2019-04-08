package goals.value_function;

import actions.Action;
import actions.ActionGenerator_FixedActions;
import actions.ActionQueue;
import actions.ActionSet;
import distributions.Distribution_Equal;
import game.GameUnified;
import tree.Node;
import tree.Utility;
import ui.PanelRunner;
import ui.ScreenCapture;
import value.ValueFunction_TensorFlow;
import value.ValueFunction_TensorFlow_StateOnly;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@SuppressWarnings("ALL")
public class MAIN_SingleEvaluation extends JPanel implements ActionListener, MouseListener, MouseMotionListener {

    GameUnified game = new GameUnified();

    boolean q = false;
    boolean w = false;
    boolean o = false;
    boolean p = false;

    public static void main(String[] args) {
        boolean doScreenCapture = false;
        ScreenCapture screenCapture = new ScreenCapture(new File(Utility.generateFileName("vid","mp4")));
        if (doScreenCapture) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    screenCapture.finalize();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));
        }

        // Set up the visualizer.
        MAIN_SingleEvaluation qwop = new MAIN_SingleEvaluation();
        qwop.game.failOnThighContact = false;
        JFrame frame = new JFrame(); // New frame to hold and manage the QWOP JPanel.
        frame.add(qwop);
        frame.setPreferredSize(new Dimension(600, 400));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
        qwop.addMouseListener(qwop);
        qwop.addMouseMotionListener(qwop);

        // Fire game update every 40 ms.
        Timer timer = new Timer(40, qwop);
        timer.start();

        // Load a value function controller.
        ValueFunction_TensorFlow valueFunction = null;
        try {
            valueFunction = new ValueFunction_TensorFlow_StateOnly(new File("src/main/resources/tflow_models" +
                    "/state_only_aftergamerevisions.pb"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        valueFunction.loadCheckpoint("chk_small39"); // chk_after565"); // chk5");

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
                new Action(7,false,false,false,false),
//                new Action(34,false,true,true,false),
//                new Action(19,false,false,false,false),
//                new Action(45,true,false,false,true),
//
//                new Action(10,false,false,false,false),
//                new Action(27,false,true,true,false),
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
            if (doScreenCapture) {
                try {
                    screenCapture.takeFrameFromContainer(frame);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Node currNode = leaf.get(0);

        // Run the controller until failure.
        while (true) { //!qwop.game.getFailureStatus()) {

            Utility.tic();
            Action chosenAction = valueFunction.getMaximizingAction(currNode, qwop.game);
            Utility.toc();

            boolean[] keys =  chosenAction.peek();
            qwop.q = keys[0];
            qwop.w = keys[1];
            qwop.o = keys[2];
            qwop.p = keys[3];
            actionQueue.addAction(chosenAction);
            while (!actionQueue.isEmpty()) {
                long currTime = System.currentTimeMillis();

                if (qwop.mouseActive) {
                    qwop.arrowShape = PanelRunner.createArrowShape(qwop.mousePoint, new Point(300,180), 80);
                    float impulseX = 300f - qwop.mousePoint.x;
                    float impulseY = 200f - qwop.mousePoint.y;
                    float impulseGain = 0.008f;
                    qwop.game.applyBodyImpulse(impulseGain * impulseX, impulseGain * impulseY);
                } else {
                    qwop.arrowShape = null;
                }

                qwop.game.step(actionQueue.pollCommand());

                if (doScreenCapture) {
                    try {
                        screenCapture.takeFrameFromContainer(frame);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else { // Screen capture is already so slow, we don't need a delay.
                    try {
                        Thread.sleep(Math.max(1, 35 - (System.currentTimeMillis() - currTime)));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            currNode = currNode.addChild(chosenAction);
            currNode.setState(qwop.game.getCurrentState());
        }
    }

    Point mousePoint;
    boolean mouseActive = false;
    Shape arrowShape;
    Color arrowColor = new Color(0,0,0,127);
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        game.draw(g, 10, 300, 200); // Redraws the game. Scaling and offsets are handpicked to work for the size of
        // the window.
        PanelRunner.keyDrawer(g, q, w, o, p, -50, 20, 240, 40);
        if (arrowShape != null) {
            ((Graphics2D) g).setColor(arrowColor);
            ((Graphics2D) g).fill(arrowShape);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        repaint();
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {
        mouseActive = true;
        mousePoint = e.getPoint();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        mouseActive = false;
    }

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    @Override
    public void mouseDragged(MouseEvent e) {
        mousePoint = e.getPoint();
    }

    @Override
    public void mouseMoved(MouseEvent e) {}
}
