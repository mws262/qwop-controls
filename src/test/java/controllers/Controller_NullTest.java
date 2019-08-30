package controllers;

import game.qwop.GameQWOP;
import game.action.Action;
import game.qwop.CommandQWOP;
import game.qwop.StateQWOP;
import org.junit.Assert;
import org.junit.Test;
import tree.node.NodeGameExplorable;
import tree.node.NodeGameGraphics;

public class Controller_NullTest {

    @Test
    public void draw() {
        IController<CommandQWOP, StateQWOP> controller = new Controller_Null<>(new Action<>(1, CommandQWOP.NONE));
        controller.draw(null, null, 0f, 0, 0); // Does nothing, but also shouldn't throw with any arguments.
    }

    @Test
    public void policy() {
        IController<CommandQWOP, StateQWOP> controller = new Controller_Null<>(new Action<>(1, CommandQWOP.NONE));
        Action<CommandQWOP> a1 = controller.policy(new NodeGameExplorable<>(GameQWOP.getInitialState()));
        Action<CommandQWOP> a2 = controller.policy(new NodeGameGraphics<>(GameQWOP.getInitialState()));
        GameQWOP game = new GameQWOP();
        game.step(true, false, false, true);
        Action<CommandQWOP> a3 = controller.policy(new NodeGameExplorable<>(game.getCurrentState()));
        Assert.assertEquals(new Action<>(1, CommandQWOP.NONE), a1);
        Assert.assertEquals(new Action<>(1, CommandQWOP.NONE), a2);
        Assert.assertEquals(new Action<>(1, CommandQWOP.NONE), a3);
    }

    @Test
    public void getCopy() {
        IController<CommandQWOP, StateQWOP> controller = new Controller_Null<>(new Action<>(1, CommandQWOP.NONE)); // Same but with copy.
        controller = controller.getCopy();

        Action<CommandQWOP> a1 = controller.policy(new NodeGameExplorable<>(GameQWOP.getInitialState()));
        Action<CommandQWOP> a2 = controller.policy(new NodeGameGraphics<>(GameQWOP.getInitialState()));
        GameQWOP game = new GameQWOP();
        game.step(true, false, false, true);
        Action<CommandQWOP> a3 = controller.policy(new NodeGameExplorable<>(game.getCurrentState()));
        Assert.assertEquals(new Action<>(1, CommandQWOP.NONE), a1);
        Assert.assertEquals(new Action<>(1, CommandQWOP.NONE), a2);
        Assert.assertEquals(new Action<>(1, CommandQWOP.NONE), a3);
    }
}