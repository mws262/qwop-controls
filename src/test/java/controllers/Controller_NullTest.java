package controllers;

import game.GameUnified;
import game.action.Action;
import org.junit.Assert;
import org.junit.Test;
import tree.node.NodeQWOPExplorable;
import tree.node.NodeQWOPGraphics;

public class Controller_NullTest {

    @Test
    public void draw() {
        IController controller = new Controller_Null();
        controller.draw(null, null, 0f, 0, 0); // Does nothing, but also shouldn't throw with any arguments.
    }

    @Test
    public void policy() {
        IController controller = new Controller_Null();
        Action a1 = controller.policy(new NodeQWOPExplorable(GameUnified.getInitialState()));
        Action a2 = controller.policy(new NodeQWOPGraphics(GameUnified.getInitialState()));
        GameUnified game = new GameUnified();
        game.step(true, false, false, true);
        Action a3 = controller.policy(new NodeQWOPExplorable(game.getCurrentState()));
        Assert.assertEquals(new Action(1, false, false, false, false), a1);
        Assert.assertEquals(new Action(1, false, false, false, false), a2);
        Assert.assertEquals(new Action(1, false, false, false, false), a3);
    }

    @Test
    public void getCopy() {
        IController controller = new Controller_Null().getCopy(); // Same but with copy.
        Action a1 = controller.policy(new NodeQWOPExplorable(GameUnified.getInitialState()));
        Action a2 = controller.policy(new NodeQWOPGraphics(GameUnified.getInitialState()));
        GameUnified game = new GameUnified();
        game.step(true, false, false, true);
        Action a3 = controller.policy(new NodeQWOPExplorable(game.getCurrentState()));
        Assert.assertEquals(new Action(1, false, false, false, false), a1);
        Assert.assertEquals(new Action(1, false, false, false, false), a2);
        Assert.assertEquals(new Action(1, false, false, false, false), a3);
    }
}