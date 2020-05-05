package goals.playback;

import data.SavableSingleGame;
import game.action.Action;
import game.action.ActionQueue;
import game.qwop.CommandQWOP;
import game.qwop.GameQWOP;
import game.qwop.StateQWOP;
import tree.node.NodeGameGraphics;
import ui.runner.PanelRunner_Snapshot;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/**
 * Playback runs or sections of runs saved in {@link SavableSingleGame} files.
 *
 * @author matt
 */

public class MAIN_PlaybackManual extends JFrame {

    PanelRunner_Snapshot<StateQWOP> panel = new PanelRunner_Snapshot<>("runner");
    ActionQueue<CommandQWOP> queue = new ActionQueue<>();
    GameQWOP game = new GameQWOP();
    private Font font = new Font("Verdana", Font.BOLD, 24);

    public void go() {
        panel.activateTab();
        getContentPane().add(panel);

        setPreferredSize(new Dimension(1000, 400));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setVisible(true);


        queue.addSequence(Arrays.asList(actions));

        while (!queue.isEmpty()) {
            game.step(queue.pollCommand());
            panel.update(new NodeGameGraphics<>(game.getCurrentState()));
            repaint();
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new MAIN_PlaybackManual().go();

    }

    public void paint(Graphics g) {
        super.paint(g);
        final int startLine = 72; // Start line in the code for the queue definition. stupid thing to do lol
        if (queue != null && !queue.isEmpty()) {
            Action act = queue.peekThisAction();
            g.setFont(font);
            g.drawString(act.toString(), 600, 200);
            g.drawString(String.valueOf(queue.getCurrentActionIdx() + startLine), 600, 250);
        }
    }



    Action<CommandQWOP>[] actions = new Action[] {
            new Action<>(14, CommandQWOP.NONE),
            new Action<>(10, CommandQWOP.WO),
            new Action<>(1, CommandQWOP.W),
            new Action<>(15, CommandQWOP.NONE),
            new Action<>(7, CommandQWOP.Q),
            new Action<>(17, CommandQWOP.QP),

            new Action<>(9, CommandQWOP.NONE),
            new Action<>(1, CommandQWOP.W),
            new Action<>(13, CommandQWOP.WO),
            new Action<>(2, CommandQWOP.W),
            new Action<>(5, CommandQWOP.WO),
            new Action<>(14, CommandQWOP.NONE), // 7
            new Action<>(4, CommandQWOP.QP),
            new Action<>(4, CommandQWOP.Q),
            new Action<>(5, CommandQWOP.QP),
            new Action<>(3, CommandQWOP.Q),
            new Action<>(6, CommandQWOP.QP),
            new Action<>(1, CommandQWOP.Q),

            new Action<>(9, CommandQWOP.NONE),
            new Action<>(6, CommandQWOP.W),
            new Action<>(3, CommandQWOP.WO),
            new Action<>(3, CommandQWOP.W),
            new Action<>(4, CommandQWOP.WO),
            new Action<>(3, CommandQWOP.W), // 3
            new Action<>(5, CommandQWOP.WO), // 5
            new Action<>(1, CommandQWOP.W),
            new Action<>(7, CommandQWOP.NONE), // 7
            new Action<>(3, CommandQWOP.P),
            new Action<>(11, CommandQWOP.QP),
            new Action<>(3, CommandQWOP.Q),
            new Action<>(6, CommandQWOP.QP),
            new Action<>(1, CommandQWOP.Q),

            new Action<>(8, CommandQWOP.NONE),
            new Action<>(12, CommandQWOP.W),
            new Action<>(4, CommandQWOP.WO),
            new Action<>(2, CommandQWOP.W),
            new Action<>(4, CommandQWOP.WO),
            new Action<>(3, CommandQWOP.W), // 27, 8
            new Action<>(8, CommandQWOP.NONE),
            new Action<>(2, CommandQWOP.Q),
            new Action<>(5, CommandQWOP.QP),
            new Action<>(8, CommandQWOP.Q),
            new Action<>(9, CommandQWOP.QP), // 24, 14

            new Action<>(5, CommandQWOP.NONE),
            new Action<>(6, CommandQWOP.WO),
            new Action<>(5, CommandQWOP.W),
            new Action<>(4, CommandQWOP.WO),
            new Action<>(4, CommandQWOP.W),
            new Action<>(4, CommandQWOP.WO),
            new Action<>(2, CommandQWOP.W), // 25, 14
            new Action<>(6, CommandQWOP.NONE),
            new Action<>(2, CommandQWOP.Q),
            new Action<>(2, CommandQWOP.QP),
            new Action<>(10, CommandQWOP.Q),
            new Action<>(5, CommandQWOP.QP),
            new Action<>(4, CommandQWOP.Q),
            new Action<>(4, CommandQWOP.QP), // 27, 11

            new Action<>(2, CommandQWOP.NONE),
            new Action<>(1, CommandQWOP.W),
            new Action<>(4, CommandQWOP.WO),
            new Action<>(5, CommandQWOP.W),
            new Action<>(3, CommandQWOP.WO),
            new Action<>(2, CommandQWOP.W),
            new Action<>(9, CommandQWOP.WO), // 24, 16
            new Action<>(1, CommandQWOP.NONE),
            new Action<>(4, CommandQWOP.QP),
            new Action<>(7, CommandQWOP.Q),
            new Action<>(3, CommandQWOP.QP),
            new Action<>(4, CommandQWOP.Q),
            new Action<>(6, CommandQWOP.QP),
            new Action<>(1, CommandQWOP.Q), // 25, 13

            new Action<>(6, CommandQWOP.NONE),
            new Action<>(14, CommandQWOP.W),
            new Action<>(4, CommandQWOP.WO),
            new Action<>(10, CommandQWOP.W),
            new Action<>(1, CommandQWOP.WO),
            new Action<>(5, CommandQWOP.O), // 34, 5
            new Action<>(8, CommandQWOP.NONE),
            new Action<>(17, CommandQWOP.Q),
            new Action<>(10, CommandQWOP.QP), // 27, 10

            new Action<>(9, CommandQWOP.NONE),
            new Action<>(1, CommandQWOP.O),
            new Action<>(3, CommandQWOP.WO),
            new Action<>(11, CommandQWOP.W),
            new Action<>(2, CommandQWOP.WO),
            new Action<>(5, CommandQWOP.W),
            new Action<>(4, CommandQWOP.NONE),
            new Action<>(5, CommandQWOP.O),
            new Action<>(11, CommandQWOP.NONE),
            new Action<>(8, CommandQWOP.Q),
            new Action<>(4, CommandQWOP.QP),
            new Action<>(7, CommandQWOP.Q),
            new Action<>(4, CommandQWOP.QP),
            new Action<>(2, CommandQWOP.Q),
            new Action<>(4, CommandQWOP.QP),

            new Action<>(10, CommandQWOP.NONE),
            new Action<>(1, CommandQWOP.W),
            new Action<>(8, CommandQWOP.WO),
            new Action<>(6, CommandQWOP.W),
            new Action<>(3, CommandQWOP.WO),
            new Action<>(3, CommandQWOP.W),
            new Action<>(4, CommandQWOP.WO),
            new Action<>(15, CommandQWOP.NONE),
            new Action<>(3, CommandQWOP.Q),
            new Action<>(7, CommandQWOP.QP),
            new Action<>(9, CommandQWOP.Q),
            new Action<>(6, CommandQWOP.QP),

            new Action<>(8, CommandQWOP.NONE),
            new Action<>(7, CommandQWOP.W),
            new Action<>(4, CommandQWOP.WO),
            new Action<>(4, CommandQWOP.W),
            new Action<>(2, CommandQWOP.WO),
            new Action<>(5, CommandQWOP.W),
            new Action<>(3, CommandQWOP.WO),
            new Action<>(1, CommandQWOP.O),
            new Action<>(6, CommandQWOP.NONE),
            new Action<>(1, CommandQWOP.Q),
            new Action<>(3, CommandQWOP.QP),
            new Action<>(12, CommandQWOP.Q),
            new Action<>(4, CommandQWOP.QP),
            new Action<>(5, CommandQWOP.Q),
            new Action<>(1, CommandQWOP.QP),
            new Action<>(2, CommandQWOP.P),

            new Action<>(12, CommandQWOP.NONE),
            new Action<>(1, CommandQWOP.W),
            new Action<>(8, CommandQWOP.WO),
            new Action<>(2, CommandQWOP.W),
            new Action<>(5, CommandQWOP.WO),
            new Action<>(4, CommandQWOP.W),
            new Action<>(4, CommandQWOP.WO),
            new Action<>(1, CommandQWOP.O),
            new Action<>(4, CommandQWOP.NONE),
            new Action<>(5, CommandQWOP.Q),
            new Action<>(1, CommandQWOP.QP),
            new Action<>(5, CommandQWOP.Q),
            new Action<>(4, CommandQWOP.QP),
            new Action<>(2, CommandQWOP.Q),
            new Action<>(12, CommandQWOP.QP),
            new Action<>(1, CommandQWOP.Q),

            new Action<>(2, CommandQWOP.NONE),
            new Action<>(2, CommandQWOP.W),
            new Action<>(11, CommandQWOP.WO),
            new Action<>(2, CommandQWOP.W),
            new Action<>(8, CommandQWOP.WO),
            new Action<>(1, CommandQWOP.W),
            new Action<>(14, CommandQWOP.NONE),
            new Action<>(1, CommandQWOP.P),
            new Action<>(8, CommandQWOP.QP),
            new Action<>(7, CommandQWOP.Q),
            new Action<>(8, CommandQWOP.QP),
            new Action<>(2, CommandQWOP.Q),

            new Action<>(6, CommandQWOP.NONE),
            new Action<>(3, CommandQWOP.WO),
            new Action<>(11, CommandQWOP.W),
            new Action<>(4, CommandQWOP.WO),
            new Action<>(4, CommandQWOP.W),
            new Action<>(3, CommandQWOP.WO),
            new Action<>(2, CommandQWOP.O),
            new Action<>(12, CommandQWOP.NONE),
            new Action<>(4, CommandQWOP.QP),
            new Action<>(15, CommandQWOP.Q),
            new Action<>(5, CommandQWOP.QP),

            new Action<>(15, CommandQWOP.NONE),
            new Action<>(7, CommandQWOP.W),
            new Action<>(7, CommandQWOP.WO),
            new Action<>(3, CommandQWOP.W),
            new Action<>(3, CommandQWOP.WO),
            new Action<>(4, CommandQWOP.W),
            new Action<>(1, CommandQWOP.WO),
            new Action<>(2, CommandQWOP.O),
            new Action<>(7, CommandQWOP.NONE),
            new Action<>(8, CommandQWOP.Q),
            new Action<>(4, CommandQWOP.QP),
            new Action<>(5, CommandQWOP.Q),
            new Action<>(8, CommandQWOP.QP),
            new Action<>(2, CommandQWOP.Q),

            new Action<>(10, CommandQWOP.NONE),
            new Action<>(3, CommandQWOP.W),
            new Action<>(12, CommandQWOP.WO),
            new Action<>(5, CommandQWOP.W),
            new Action<>(5, CommandQWOP.WO),
            new Action<>(3, CommandQWOP.NONE),
            new Action<>(2, CommandQWOP.QP),
            new Action<>(5, CommandQWOP.Q),
            new Action<>(4, CommandQWOP.QP),
            new Action<>(2, CommandQWOP.Q),
            new Action<>(12, CommandQWOP.QP),
            new Action<>(1, CommandQWOP.Q),

            new Action<>(5, CommandQWOP.NONE),
            new Action<>(1, CommandQWOP.W),
            new Action<>(5, CommandQWOP.WO),
            new Action<>(5, CommandQWOP.W),
            new Action<>(4, CommandQWOP.WO),
            new Action<>(4, CommandQWOP.W),
            new Action<>(2, CommandQWOP.WO),
            new Action<>(3, CommandQWOP.O),
            new Action<>(2, CommandQWOP.NONE),
            new Action<>(5, CommandQWOP.QP),
            new Action<>(5, CommandQWOP.Q),
            new Action<>(4, CommandQWOP.QP),
            new Action<>(4, CommandQWOP.Q),
            new Action<>(7, CommandQWOP.QP),
            new Action<>(1, CommandQWOP.Q),

            new Action<>(4, CommandQWOP.NONE),
            new Action<>(14, CommandQWOP.WO),
            new Action<>(2, CommandQWOP.W),
            new Action<>(4, CommandQWOP.WO),
            new Action<>(4, CommandQWOP.W),
            new Action<>(2, CommandQWOP.WO),
            new Action<>(2, CommandQWOP.O),
            new Action<>(5, CommandQWOP.NONE),
            new Action<>(12, CommandQWOP.Q),
            new Action<>(4, CommandQWOP.QP),
            new Action<>(2, CommandQWOP.Q),
            new Action<>(15, CommandQWOP.QP),
            new Action<>(2, CommandQWOP.Q),

            new Action<>(2, CommandQWOP.NONE),
            new Action<>(1, CommandQWOP.W),
            new Action<>(9, CommandQWOP.WO),
            new Action<>(5, CommandQWOP.W),
            new Action<>(3, CommandQWOP.WO),
            new Action<>(2, CommandQWOP.W),
            new Action<>(6, CommandQWOP.WO),
            new Action<>(2, CommandQWOP.W),
            new Action<>(15, CommandQWOP.NONE),
            new Action<>(13, CommandQWOP.QP),
            new Action<>(4, CommandQWOP.Q),
            new Action<>(5, CommandQWOP.QP),

            new Action<>(5, CommandQWOP.NONE),
            new Action<>(11, CommandQWOP.W),
            new Action<>(4, CommandQWOP.WO),
            new Action<>(3, CommandQWOP.W),
            new Action<>(11, CommandQWOP.WO),
            new Action<>(1, CommandQWOP.O),
            new Action<>(1, CommandQWOP.NONE),
            new Action<>(2, CommandQWOP.Q),
            new Action<>(5, CommandQWOP.QP),
            new Action<>(10, CommandQWOP.Q),
            new Action<>(5, CommandQWOP.QP),
            new Action<>(1, CommandQWOP.Q),

            new Action<>(7, CommandQWOP.NONE),
            new Action<>(3, CommandQWOP.W),
            new Action<>(4, CommandQWOP.WO),
            new Action<>(7, CommandQWOP.W),
            new Action<>(3, CommandQWOP.WO),
            new Action<>(5, CommandQWOP.W),
            new Action<>(12, CommandQWOP.NONE),
            new Action<>(15, CommandQWOP.Q),
            new Action<>(3, CommandQWOP.QP),
            new Action<>(3, CommandQWOP.Q),
            new Action<>(4, CommandQWOP.QP),

            new Action<>(9, CommandQWOP.NONE),
            new Action<>(2, CommandQWOP.W),
            new Action<>(4, CommandQWOP.WO),
            new Action<>(6, CommandQWOP.W),
            new Action<>(3, CommandQWOP.WO),
            new Action<>(4, CommandQWOP.W),
            new Action<>(2, CommandQWOP.WO),
            new Action<>(5, CommandQWOP.W),
            new Action<>(3, CommandQWOP.O),
            new Action<>(8, CommandQWOP.NONE),
            new Action<>(2, CommandQWOP.Q),
            new Action<>(3, CommandQWOP.QP),
            new Action<>(12, CommandQWOP.Q),
            new Action<>(7, CommandQWOP.QP),
            new Action<>(1, CommandQWOP.Q),
            new Action<>(5, CommandQWOP.NONE),
            new Action<>(2, CommandQWOP.O),
            new Action<>(8, CommandQWOP.WO),
            new Action<>(4, CommandQWOP.W),
            new Action<>(5, CommandQWOP.WO),
            new Action<>(3, CommandQWOP.W),
            new Action<>(1, CommandQWOP.WO),
            new Action<>(1, CommandQWOP.O),
            new Action<>(6, CommandQWOP.NONE),
            new Action<>(3, CommandQWOP.Q),
            new Action<>(7, CommandQWOP.QP),
            new Action<>(5, CommandQWOP.Q),
            new Action<>(10, CommandQWOP.QP),
            new Action<>(2, CommandQWOP.Q),
            new Action<>(10, CommandQWOP.NONE),
            new Action<>(1, CommandQWOP.W),
            new Action<>(11, CommandQWOP.WO),
            new Action<>(5, CommandQWOP.W),
            new Action<>(5, CommandQWOP.WO),
            new Action<>(1, CommandQWOP.W),
            new Action<>(9, CommandQWOP.NONE),
            new Action<>(11, CommandQWOP.QP),
            new Action<>(5, CommandQWOP.Q),
            new Action<>(9, CommandQWOP.QP),
            new Action<>(6, CommandQWOP.NONE),
            new Action<>(2, CommandQWOP.W),
            new Action<>(8, CommandQWOP.WO),
            new Action<>(4, CommandQWOP.W),
            new Action<>(4, CommandQWOP.WO),
            new Action<>(5, CommandQWOP.W),
            new Action<>(6, CommandQWOP.NONE),
            new Action<>(4, CommandQWOP.QP),
            new Action<>(12, CommandQWOP.Q),
            new Action<>(9, CommandQWOP.QP),
            new Action<>(11, CommandQWOP.NONE),
            new Action<>(3, CommandQWOP.W),
            new Action<>(9, CommandQWOP.WO),
            new Action<>(7, CommandQWOP.W),
            new Action<>(5, CommandQWOP.WO),
            new Action<>(15, CommandQWOP.NONE),
            new Action<>(1, CommandQWOP.Q),
            new Action<>(10, CommandQWOP.QP),
            new Action<>(5, CommandQWOP.Q),
            new Action<>(9, CommandQWOP.QP),
            new Action<>(4, CommandQWOP.NONE),
            new Action<>(6, CommandQWOP.WO),
            new Action<>(3, CommandQWOP.W),
            new Action<>(2, CommandQWOP.WO),
            new Action<>(5, CommandQWOP.W),
            new Action<>(5, CommandQWOP.WO),
            new Action<>(3, CommandQWOP.W),
            new Action<>(12, CommandQWOP.NONE),
            new Action<>(10, CommandQWOP.QP),
            new Action<>(5, CommandQWOP.Q),
            new Action<>(5, CommandQWOP.QP),
            new Action<>(3, CommandQWOP.Q),
            new Action<>(7, CommandQWOP.NONE),
            new Action<>(13, CommandQWOP.WO),
            new Action<>(6, CommandQWOP.W),
            new Action<>(4, CommandQWOP.WO),
            new Action<>(1, CommandQWOP.W),
            new Action<>(12, CommandQWOP.NONE),
            new Action<>(4, CommandQWOP.Q),
            new Action<>(11, CommandQWOP.QP),
            new Action<>(4, CommandQWOP.Q),
            new Action<>(5, CommandQWOP.QP),
            new Action<>(1, CommandQWOP.Q),
            new Action<>(9, CommandQWOP.NONE),
            new Action<>(5, CommandQWOP.WO),
            new Action<>(5, CommandQWOP.W),
            new Action<>(4, CommandQWOP.WO),
            new Action<>(4, CommandQWOP.W),
            new Action<>(5, CommandQWOP.WO),
            new Action<>(7, CommandQWOP.NONE),
            new Action<>(10, CommandQWOP.Q),
            new Action<>(4, CommandQWOP.QP),
            new Action<>(2, CommandQWOP.Q),
            new Action<>(7, CommandQWOP.QP),
            new Action<>(2, CommandQWOP.Q),
            new Action<>(5, CommandQWOP.QP),
            new Action<>(8, CommandQWOP.NONE),
            new Action<>(1, CommandQWOP.W),
            new Action<>(10, CommandQWOP.WO),
            new Action<>(4, CommandQWOP.W),
            new Action<>(3, CommandQWOP.WO),
            new Action<>(4, CommandQWOP.W),
            new Action<>(9, CommandQWOP.NONE),
            new Action<>(1, CommandQWOP.P),
            new Action<>(16, CommandQWOP.Q),
            new Action<>(5, CommandQWOP.QP),
            new Action<>(2, CommandQWOP.Q),
            new Action<>(10, CommandQWOP.NONE),
            new Action<>(13, CommandQWOP.W),
            new Action<>(4, CommandQWOP.WO),
            new Action<>(4, CommandQWOP.W),
            new Action<>(4, CommandQWOP.WO),
            new Action<>(1, CommandQWOP.O),
            new Action<>(4, CommandQWOP.NONE),
            new Action<>(2, CommandQWOP.QP),
            new Action<>(5, CommandQWOP.Q),
            new Action<>(5, CommandQWOP.QP),
            new Action<>(4, CommandQWOP.Q),
            new Action<>(10, CommandQWOP.QP),
            new Action<>(1, CommandQWOP.P),
            new Action<>(4, CommandQWOP.NONE),
            new Action<>(2, CommandQWOP.O),
            new Action<>(19, CommandQWOP.WO),
            new Action<>(1, CommandQWOP.O),
            new Action<>(2, CommandQWOP.P),
            new Action<>(17, CommandQWOP.QP)
    };
}
