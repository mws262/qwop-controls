package tree.node.evaluator;

import game.action.Action;
import game.qwop.CommandQWOP;
import game.qwop.GameQWOP;
import game.qwop.StateQWOP;
import org.junit.Assert;
import org.junit.Test;
import tree.node.NodeGame;

public class EvaluationFunction_VelocityTest {

    @Test
    public void getValue() {

        IEvaluationFunction<CommandQWOP, StateQWOP> evalFun = new EvaluationFunction_Velocity<>();
        GameQWOP game = new GameQWOP();

        NodeGame<CommandQWOP, StateQWOP> root = new NodeGame<>(GameQWOP.getInitialState());

        Assert.assertEquals(GameQWOP.getInitialState().body.getDx(), evalFun.getValue(root), 1e-12f);

        game.step(CommandQWOP.QO);
        StateQWOP stateAfterStep = game.getCurrentState();
        NodeGame<CommandQWOP, StateQWOP> nodeAfterStep = root.addBackwardsLinkedChild(new Action<>(1, CommandQWOP.QO),
                stateAfterStep);

        Assert.assertEquals(stateAfterStep.body.getDx(), evalFun.getValue(nodeAfterStep), 1e-12f);
    }
}