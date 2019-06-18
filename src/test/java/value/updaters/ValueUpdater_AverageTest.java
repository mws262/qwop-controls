package value.updaters;

import game.state.IState;
import org.junit.Assert;
import org.junit.Test;
import tree.node.NodeQWOP;
import tree.node.NodeQWOPBase;
import tree.node.NodeQWOPExplorable;
import tree.node.NodeQWOPGraphics;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ValueUpdater_AverageTest {

    @Test
    public void update() {
        IState st = mock(IState.class);
        when(st.isFailed()).thenReturn(false);

        // Do tests for each kind of node that could use this updater.
        NodeQWOPBase n = new NodeQWOP(st);
        runUpdatesOnNode(n);

        n = new NodeQWOPExplorable(st);
        runUpdatesOnNode(n);

        n = new NodeQWOPGraphics(st);
        runUpdatesOnNode(n);
    }

    private void runUpdatesOnNode(NodeQWOPBase n) {
        float tol = 1e-10f;
        ValueUpdater_Average valUpdater = new ValueUpdater_Average();

        // After no updates.
        Assert.assertEquals(0f, n.getValue(), tol);
        Assert.assertEquals(0, n.getUpdateCount());

        // After 1 update.
        n.updateValue(0.5f, valUpdater);
        Assert.assertEquals(1, n.getUpdateCount());
        Assert.assertEquals(0.5f, n.getValue(), tol);

        // After 2 updates.
        n.updateValue(0.1f, valUpdater);
        Assert.assertEquals(0.3f, n.getValue(), tol);
        Assert.assertEquals(2, n.getUpdateCount());

        // After 3 updates.
        n.updateValue(0f, valUpdater);
        Assert.assertEquals(0.2f, n.getValue(), tol);
    }
}