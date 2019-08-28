package controllers;

import game.qwop.GameQWOP;
import game.action.Action;
import game.qwop.CommandQWOP;
import org.junit.Assert;
import org.junit.Test;
import tree.node.NodeQWOPExplorable;
import tree.node.NodeQWOPGraphics;

public class Controller_NullTest {

    @Test
    public void draw() {
        IController<CommandQWOP> controller = new Controller_Null<>(new Action<>(1, CommandQWOP.NONE));
        controller.draw(null, null, 0f, 0, 0); // Does nothing, but also shouldn't throw with any arguments.
    }

    @Test
    public void policy() {
        IController<CommandQWOP> controller = new Controller_Null<>(new Action<>(1, CommandQWOP.NONE));
        Action<CommandQWOP> a1 = controller.policy(new NodeQWOPExplorable<>(GameQWOP.getInitialState()));
        Action<CommandQWOP> a2 = controller.policy(new NodeQWOPGraphics<>(GameQWOP.getInitialState()));
        GameQWOP game = new GameQWOP();
        game.step(true, false, false, true);
        Action<CommandQWOP> a3 = controller.policy(new NodeQWOPExplorable<>(game.getCurrentState()));
        Assert.assertEquals(new Action<>(1, CommandQWOP.NONE), a1);
        Assert.assertEquals(new Action<>(1, CommandQWOP.NONE), a2);
        Assert.assertEquals(new Action<>(1, CommandQWOP.NONE), a3);
    }

    @Test
    public void getCopy() {
        IController<CommandQWOP> controller = new Controller_Null<>(new Action<>(1, CommandQWOP.NONE)); // Same but with copy.
        controller = controller.getCopy();

        Action<CommandQWOP> a1 = controller.policy(new NodeQWOPExplorable<>(GameQWOP.getInitialState()));
        Action<CommandQWOP> a2 = controller.policy(new NodeQWOPGraphics<>(GameQWOP.getInitialState()));
        GameQWOP game = new GameQWOP();
        game.step(true, false, false, true);
        Action<CommandQWOP> a3 = controller.policy(new NodeQWOPExplorable<>(game.getCurrentState()));
        Assert.assertEquals(new Action<>(1, CommandQWOP.NONE), a1);
        Assert.assertEquals(new Action<>(1, CommandQWOP.NONE), a2);
        Assert.assertEquals(new Action<>(1, CommandQWOP.NONE), a3);
    }
}