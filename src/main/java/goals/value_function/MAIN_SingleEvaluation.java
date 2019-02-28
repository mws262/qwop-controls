package goals.value_function;

import actions.Action;
import actions.ActionQueue;
import actions.ActionSet;
import game.GameSingleThreadWithDraw;
import game.GameThreadSafe;
import goals.tree_search.MAIN_Search_Template;
import tflowtools.TrainableNetwork;
import tree.Node;
import value.ValueFunction_TensorFlow_ActionIn;
import value.ValueFunction_TensorFlow_ActionInMulti;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static goals.tree_search.MAIN_Search_Template.assignAllowableActionsWider;

@SuppressWarnings("ALL")
public class MAIN_SingleEvaluation extends JPanel implements ActionListener {

    GameSingleThreadWithDraw game = new GameSingleThreadWithDraw();


    public static void main(String[] args) {

        MAIN_SingleEvaluation qwop = new MAIN_SingleEvaluation();

        JFrame frame = new JFrame(); // New frame to hold and manage the QWOP JPanel.
        frame.add(qwop);
        frame.setPreferredSize(new Dimension(600, 400));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        // Fire game update every 40 ms.
        new Timer(40, qwop).start();

//        ValueFunction_TensorFlow_ActionIn valueFunction = new ValueFunction_TensorFlow_ActionIn(new File("src/main/resources/tflow_models/tmp3.pb"));
//        valueFunction.loadCheckpoint("chk3");
        assignAllowableActionsWider(-1);
        ValueFunction_TensorFlow_ActionInMulti valueFunction =
                ValueFunction_TensorFlow_ActionInMulti.loadExisting(Node.potentialActionGenerator.getAllPossibleActions(), "tmpMulti", "src/main/resources/tflow_models/");
        valueFunction.loadCheckpoints("chk");

        assignAllowableActionsWider(-1);
        Node rootNode = new Node();

        List<Action[]> alist = new ArrayList<>();
        alist.add(new Action[]{
                new Action(1,false,false,false,false),
                new Action(34,false,true,true,false),
                new Action(19,false,false,false,false),
                new Action(45,true,false,false,true),

                new Action(10,false,false,false,false),
                new Action(27,false,true,true,false),
                new Action(8,false,false,false,false),
                new Action(20,true,false,false,true),
        });

        Node.makeNodesFromActionSequences(alist, rootNode, new GameThreadSafe());
        Node.stripUncheckedActionsExceptOnLeaves(rootNode, 7);

        List<Node> leaf = new ArrayList<>();
        rootNode.getLeaves(leaf);

        ActionQueue actionQueue = new ActionQueue();
        actionQueue.addSequence(leaf.get(0).getSequence());

        while (!actionQueue.isEmpty()) {
            qwop.game.step(actionQueue.pollCommand());
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Node currNode = leaf.get(0);

        float[][] state = new float[1][73];
        while (!qwop.game.getFailureStatus()) {

            Action chosenAction = valueFunction.getMaximizingAction(currNode);
            actionQueue.addAction(chosenAction);
            while (!actionQueue.isEmpty()) {
               // qwop.game.applyBodyImpulse(-3f, 0.001f);
                qwop.game.step(actionQueue.pollCommand());
                try {
                    Thread.sleep(40);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
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
