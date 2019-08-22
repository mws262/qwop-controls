package goals.value_function;

import game.GameUnified;
import game.action.Action;
import game.action.ActionQueue;
import game.action.CommandQWOP;
import game.state.IState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tree.Utility;
import tree.node.NodeQWOPExplorable;
import ui.runner.PanelRunner;
import value.ValueFunction_TensorFlow;
import value.ValueFunction_TensorFlow_StateOnly;
import vision.ScreenCapture;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("ALL")
public class MAIN_SingleEvaluation extends JPanel implements ActionListener, MouseListener, MouseMotionListener {

    static {
        Utility.loadLoggerConfiguration();
    }

    GameUnified game = new GameUnified(); // new GameUnifiedCaching(1,2);

    private boolean doFullGameSerialization = false;

    // Net and execution parameters.
    String valueNetworkName = "src/main/resources/tflow_models/tuesday.pb";
    String checkpointName = "src/main/resources/tflow_models/checkpoints/small329"; // "med67";
    private boolean doScreenCapture = false;

    // Game and controller fields.
    private ActionQueue<CommandQWOP> actionQueue = new ActionQueue();
    private ValueFunction_TensorFlow valueFunction;

    // Drawing parameters.
    private ScreenCapture screenCapture;

    private final JFrame frame = new JFrame(); // New frame to hold and manage the QWOP JPanel.
    private Point mousePoint;
    private boolean mouseActive = false;
    private Shape arrowShape;
    private final Color arrowColor = new Color(0,0,0,127);

    private static final float
            centerX = 600,
            centerY = 400;

    private boolean
            q = false,
            w = false,
            o = false,
            p = false;

    private static final Logger logger = LogManager.getLogger(MAIN_SingleEvaluation.class);

    MAIN_SingleEvaluation() {
        /* Set up screen capture, if enabled. */
        if (doScreenCapture) {
            screenCapture = new ScreenCapture(new File(Utility.generateFileName("vid","mp4")));
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    screenCapture.finish();
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
        try {
            valueFunction =
                    new ValueFunction_TensorFlow_StateOnly(new File(valueNetworkName), game, false);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Objects.requireNonNull(valueFunction);
        try {
            valueFunction.loadCheckpoint(checkpointName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private NodeQWOPExplorable doPrefix() {

        NodeQWOPExplorable rootNode = new NodeQWOPExplorable(GameUnified.getInitialState());

        // Assign a "prefix" of game.action, since I'm not sure if the controller will generalize to this part of running.
        List<Action<CommandQWOP>[]> alist = new ArrayList<>();
        alist.add(new Action[]{
                new Action<>(7, CommandQWOP.NONE),
//                new Action(34, Action.Keys.wo),
//                new Action(19, Action.Keys.none),
//                new Action(20, Action.Keys.qp),
////
//                new Action(10,false,false,false,false),
//                new Action(27,false,true,true,false),
//                 new Action(8,false,false,false,false),
//                new Action(20,true,false,false,true),
        });

        NodeQWOPExplorable.makeNodesFromActionSequences(alist, rootNode, game);

        List<NodeQWOPExplorable> leaf = new ArrayList<>();
        rootNode.getLeaves(leaf);

        List<Action<CommandQWOP>> actionList = new ArrayList<>();

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
        while (currentNode.getState().getStateVariableFromName(IState.ObjectName.BODY).getY() < 30) { // Ends if the
            // runner
            // falls off the
            // edge of the world.
            // Does not end on falling, as we might want to see its behavior.
            Action<CommandQWOP> chosenAction;
            if (doFullGameSerialization) {
                chosenAction = valueFunction.getMaximizingAction(currentNode, game);
            } else {
                chosenAction = valueFunction.getMaximizingAction(currentNode);
            }

            boolean[] keys =  chosenAction.peek().get();
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
                    float impulseGain = 0.012f;
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
            currentNode = currentNode.addBackwardsLinkedChild(chosenAction, game.getCurrentState());
        }
        logger.warn("Game complete. Runner has gone off the edge of the world.");
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
