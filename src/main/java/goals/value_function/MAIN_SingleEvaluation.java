package goals.value_function;

import actions.Action;
import actions.ActionQueue;
import game.GameUnified;
import tree.NodeQWOPExplorable;
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

@SuppressWarnings("ALL")
public class MAIN_SingleEvaluation extends JPanel implements ActionListener, MouseListener, MouseMotionListener {

    static{
        Utility.loadLoggerConfiguration();
    }

    // Net and execution parameters.
    String valueNetworkName = "small_net.pb";
    String checkpointName = "med67";
    private boolean doScreenCapture = false;

    // Game and controller fields.
    private final GameUnified game = new GameUnified();
    private ActionQueue actionQueue = new ActionQueue();
    private ValueFunction_TensorFlow valueFunction;

    // Drawing parameters.
    private ScreenCapture screenCapture;

    private final JFrame frame = new JFrame(); // New frame to hold and manage the QWOP JPanel.
    private Point mousePoint;
    private boolean mouseActive = false;
    private Shape arrowShape;
    private final Color arrowColor = new Color(0,0,0,127);

    private static final float centerX = 600;
    private static final float centerY = 400;

    private boolean q = false;
    private boolean w = false;
    private boolean o = false;
    private boolean p = false;

    MAIN_SingleEvaluation() {

        /* Set up screen capture, if enabled. */
        if (doScreenCapture) {
            screenCapture = new ScreenCapture(new File(Utility.generateFileName("vid","mp4")));
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    screenCapture.finalize();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));
        }

        /* Set up the visualizer. */
        game.failOnThighContact = false;
        frame.add(this);
        frame.setPreferredSize(new Dimension(1300, 800));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
        addMouseListener(this);
        addMouseMotionListener(this);

        Timer timer = new Timer(15, this); // Fire game graphics update on a schedule.
        timer.start();


        /* Load a value function controller. */
        valueFunction = null;
        try {
            valueFunction = new ValueFunction_TensorFlow_StateOnly(new File("src/main/resources/tflow_models/" + valueNetworkName));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        valueFunction.loadCheckpoint(checkpointName);
    }

    private NodeQWOPExplorable doPrefix() {

        NodeQWOPExplorable rootNode = new NodeQWOPExplorable(GameUnified.getInitialState());

        // Assign a "prefix" of actions, since I'm not sure if the controller will generalize to this part of running.
        List<Action[]> alist = new ArrayList<>();
        alist.add(new Action[]{
                new Action(6, Action.Keys.none),
//                new Action(34, Action.Keys.wo),
//                new Action(19, Action.Keys.none),
//                new Action(20, Action.Keys.qp),
////
//                new Action(10,false,false,false,false),
//                new Action(27,false,true,true,false),
//                 new Action(8,false,false,false,false),
//                new Action(20,true,false,false,true),
        });

        NodeQWOPExplorable.makeNodesFromActionSequences(alist, rootNode, new GameUnified());

        List<NodeQWOPExplorable> leaf = new ArrayList<>();
        rootNode.getLeaves(leaf);

        List<Action> actionList = new ArrayList<>();

        NodeQWOPExplorable currNode = leaf.get(0);
        currNode.getSequence(actionList);
        actionQueue.addSequence(actionList);

        // Run the "prefix" section.
        while (!actionQueue.isEmpty()) {
            game.step(actionQueue.pollCommand());
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
        return currNode;
    }

    private void doControlled(NodeQWOPExplorable currentNode) {

        // Run the controller until failure.
        while (true) { //!qwop.game.getFailureStatus()) {

            Action chosenAction = valueFunction.getMaximizingAction(currentNode);//, game);

            boolean[] keys =  chosenAction.peek();
            q = keys[0];
            w = keys[1];
            o = keys[2];
            p = keys[3];
            actionQueue.addAction(chosenAction);
            while (!actionQueue.isEmpty()) {
                long currTime = System.currentTimeMillis();

                if (mouseActive) {
                    arrowShape = PanelRunner.createArrowShape(mousePoint, new Point((int) centerX, (int) centerY), 80);
                    float impulseX = centerX - mousePoint.x;
                    float impulseY = centerY - mousePoint.y;
                    float impulseGain = 0.008f;
                    game.applyBodyImpulse(impulseGain * impulseX, impulseGain * impulseY);
                } else {
                    arrowShape = null;
                }

                game.step(actionQueue.pollCommand());

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
            currentNode = currentNode.addDoublyLinkedChild(chosenAction, game.getCurrentState());
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        game.draw(g, 20, (int) centerX, (int) centerY); // Redraws the game. Scaling and offsets are handpicked to work
        // for the size of the window.
        PanelRunner.keyDrawer(g, q, w, o, p, -50, 40, 480, 80);
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

    public static void main(String[] args) {
        MAIN_SingleEvaluation controlledGame = new MAIN_SingleEvaluation();
        NodeQWOPExplorable currentNode = controlledGame.doPrefix();
        controlledGame.doControlled(currentNode);
    }
}
