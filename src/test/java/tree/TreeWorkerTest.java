package tree;

import game.IGameInternal;
import game.action.Action;
import game.action.ActionGenerator_FixedSequence;
import game.qwop.CommandQWOP;
import game.qwop.GameQWOP;
import game.qwop.StateQWOP;
import org.junit.Assert;
import org.junit.Test;
import savers.IDataSaver;
import tree.TreeWorker.Status;
import tree.node.NodeGameBase;
import tree.node.NodeGameExplorable;
import tree.node.NodeGameExplorableBase;
import tree.sampler.ISampler;

import java.util.List;

public class TreeWorkerTest {
    TestGame game = new TestGame();
    TestSampler sampler = new TestSampler();
    TestSaver saver = new TestSaver();
    TreeWorker<CommandQWOP, StateQWOP> tw = new TreeWorker<>(game, sampler, saver);


    @Test
    public void testTest() {

      tw.setRoot(TestSampler.root);

        Assert.assertEquals(Status.IDLE, tw.getCurrentWorkerStatus());
        tw.run();
        Assert.assertEquals(Status.IDLE, tw.getCurrentWorkerStatus());
        tw.unpauseWorker();
        tw.run();
        Assert.assertEquals(Status.INITIALIZE, tw.getCurrentWorkerStatus());
        Assert.assertFalse(saver.initializationReported());
        tw.run();
        Assert.assertTrue(saver.initializationReported());

        Assert.assertEquals(Status.TREE_POLICY_CHOOSING, tw.getCurrentWorkerStatus());
        tw.run();
        Assert.assertEquals(TestSampler.treePolicyNode, tw.getExpansionNode());
        Assert.assertEquals(Status.TREE_POLICY_EXECUTING, tw.getCurrentWorkerStatus());

        int ts = tw.getExpansionNode().getAction().getTimestepsTotal();
        for (int i = 0; i < ts; i++) {
            Assert.assertEquals(Status.TREE_POLICY_EXECUTING, tw.getCurrentWorkerStatus());
            tw.run();

        }
        Assert.assertEquals(Status.EXPANSION_POLICY_CHOOSING, tw.getCurrentWorkerStatus());






    }



    private static class TestSampler implements ISampler<CommandQWOP, StateQWOP> {

        static NodeGameExplorable<CommandQWOP, StateQWOP> root = new NodeGameExplorable<>(GameQWOP.getInitialState(),
                ActionGenerator_FixedSequence.makeDefaultGenerator(-1));

        static NodeGameExplorable<CommandQWOP, StateQWOP> treePolicyNode =
                root.addDoublyLinkedChild(new Action<>(5, CommandQWOP.Q), GameQWOP.getInitialState());

        @Override
        public NodeGameExplorableBase<?, CommandQWOP, StateQWOP> treePolicy(NodeGameExplorableBase<?, CommandQWOP, StateQWOP> startNode) {
            treePolicyNode.reserveExpansionRights();
            return treePolicyNode;
        }

        @Override
        public void treePolicyActionDone(NodeGameExplorableBase<?, CommandQWOP, StateQWOP> currentNode) {}

        @Override
        public boolean treePolicyGuard(NodeGameExplorableBase<?, CommandQWOP, StateQWOP> currentNode) {
            return false;
        }

        @Override
        public Action<CommandQWOP> expansionPolicy(NodeGameExplorableBase<?, CommandQWOP, StateQWOP> startNode) {
            return null;
        }

        @Override
        public void expansionPolicyActionDone(NodeGameExplorableBase<?, CommandQWOP, StateQWOP> currentNode) {}

        @Override
        public boolean expansionPolicyGuard(NodeGameExplorableBase<?, CommandQWOP, StateQWOP> currentNode) {
            return false;
        }

        @Override
        public void rolloutPolicy(NodeGameExplorableBase<?, CommandQWOP, StateQWOP> startNode, IGameInternal<CommandQWOP, StateQWOP> game) {}

        @Override
        public boolean rolloutPolicyGuard(NodeGameExplorableBase<?, CommandQWOP, StateQWOP> currentNode) {
            return false;
        }

        @Override
        public ISampler<CommandQWOP, StateQWOP> getCopy() {
            return null;
        }

        @Override
        public void close() {}
    }

    private static class TestSaver implements IDataSaver<CommandQWOP, StateQWOP> {

        boolean initialized = false;

        @Override
        public void reportGameInitialization(StateQWOP initialState) {
            initialized = true;
        }

        public boolean initializationReported() {
            boolean status = initialized;
            initialized = false;
            return status;
        }

        @Override
        public void reportTimestep(Action<CommandQWOP> action, IGameInternal<CommandQWOP, StateQWOP> game) {}

        @Override
        public void reportGameEnding(NodeGameBase<?, CommandQWOP, StateQWOP> endNode) {}

        @Override
        public void reportStageEnding(NodeGameBase<?, CommandQWOP, StateQWOP> rootNode, List<NodeGameBase<?, CommandQWOP, StateQWOP>> targetNodes) {}

        @Override
        public void finalizeSaverData() {}

        @Override
        public void setSaveInterval(int numGames) {}

        @Override
        public int getSaveInterval() {
            return 0;
        }

        @Override
        public void setSavePath(String fileLoc) {}

        @Override
        public String getSavePath() {
            return null;
        }

        @Override
        public savers.IDataSaver<CommandQWOP, StateQWOP> getCopy() {
            return null;
        }
    }

    private static class TestGame extends GameQWOP implements IGameInternal<CommandQWOP, StateQWOP> {

    }
}