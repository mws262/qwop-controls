package tree.sampler.rollout;

import controllers.Controller_Random;
import game.GameUnified;
import game.IGameInternal;
import game.state.IState;
import org.junit.Assert;
import org.junit.Test;
import tree.node.NodeQWOPBase;
import tree.node.NodeQWOPExplorable;
import tree.node.evaluator.EvaluationFunction_Constant;
import tree.node.evaluator.EvaluationFunction_Distance;
import tree.node.evaluator.IEvaluationFunction;

public class RolloutPolicy_DecayingHorizonRandomTest {

    @Test
    public void rolloutConstantValue() {
        IGameInternal fakeGame = new NoFailGame();
        RolloutPolicy_DecayingHorizon.kernelCenter = 0.5f;
        RolloutPolicy_DecayingHorizon.kernelSteepness = 5;
        int maxTimestepsToSim = 200;

        NodeQWOPExplorable startNode = new NodeQWOPExplorable(GameUnified.getInitialState());

        RolloutPolicy_DecayingHorizon rollout = new RolloutPolicy_DecayingHorizon(new EvaluationFunction_Constant(5),
                new Controller_Random(), maxTimestepsToSim);
        long initialGameSteps = fakeGame.getTimestepsThisGame();
        float result = rollout.rollout(startNode, fakeGame);
        Assert.assertEquals(maxTimestepsToSim, fakeGame.getTimestepsThisGame() - initialGameSteps);

        Assert.assertEquals(0, result, 1e-12f); // If the evaluator only returns one value, then the differential
        // between start and end will always be zero regardless of the weighting.
    }

    @Test
    public void rolloutArbitraryEvalFun() {
        // Make up an evaluation function and compare results with the MATLAB script:
        /* format long;
            format long;
            kernelCenter = 0.5;
            kernelSteepness = 5;
            kernel = @(ts)(-0.5 * tanh(kernelSteepness * (ts - kernelCenter)) + 0.5);
            maxTs = 200;

            accumulatedValue = 0;
            prevValue = 0;
            for i = 0:maxTs-1
                nextMultiplier = kernel(i/(maxTs - 1));
                thisValue = (i + 1);
                accumulatedValue = accumulatedValue + nextMultiplier * (thisValue - prevValue);
                prevValue = thisValue;
            end
            disp(accumulatedValue);
        */
        IGameInternal fakeGame = new NoFailGame();
        NodeQWOPExplorable startNode = new NodeQWOPExplorable(GameUnified.getInitialState());

        RolloutPolicy_DecayingHorizon.kernelCenter = 0.5f;
        RolloutPolicy_DecayingHorizon.kernelSteepness = 5;
        int maxTimestepsToSim = 200;
        RolloutPolicy_DecayingHorizon rollout = new RolloutPolicy_DecayingHorizon(new IEvaluationFunction() {
            @Override
            public float getValue(NodeQWOPBase<?> nodeToEvaluate) { return nodeToEvaluate.getTreeDepth(); }
            @Override
            public String getValueString(NodeQWOPBase<?> nodeToEvaluate) { return null; }
            @Override
            public IEvaluationFunction getCopy() { return null; }
        }, new Controller_Random(), maxTimestepsToSim);

        fakeGame.makeNewWorld();
        float result = rollout.rollout(startNode, fakeGame);

        Assert.assertEquals(100f, result, 1e-4f); // Value calculated with MATLAB script.
        // TODO is floating point error this big?
    }

    @Test
    public void rolloutKernelExtremes() {
        // If we alter the kernel to be essentially unity, then the result should be the same as just evaluating the
        // first and last states and taking the difference.
        RolloutPolicy_DecayingHorizon.kernelCenter = Float.MAX_VALUE; // Move the kernel center out very far. This
        int maxTimestepsToSim = 200;

        // should result in essentially an identity function for small inputs.
        RolloutPolicy_DecayingHorizon rollout =
                new RolloutPolicy_DecayingHorizon(new EvaluationFunction_Distance(), new Controller_Random(), maxTimestepsToSim);

        GameUnified realGame = new GameUnified();
        NodeQWOPExplorable startNode = new NodeQWOPExplorable(GameUnified.getInitialState());

        float startDistance = realGame.getCurrentState().getCenterX();
        float finalValue = rollout.rollout(startNode, realGame);
        float finalDistance = realGame.getCurrentState().getCenterX();
        Assert.assertEquals(finalDistance - startDistance, finalValue, 1e-6f);

        // If we make the kernel steep enough, then it will cut off exactly halfway through.
        RolloutPolicy_DecayingHorizon.kernelCenter = 0.5f;
        RolloutPolicy_DecayingHorizon.kernelSteepness = 1e25f;


        CacheHalfwayPoint cachingGame = new CacheHalfwayPoint(false, maxTimestepsToSim); // Will go to full 200 timesteps.
        startDistance = cachingGame.getCurrentState().getCenterX();
        finalValue = rollout.rollout(startNode, cachingGame);
        Assert.assertEquals(cachingGame.halfwayCachedState.getCenterX() - startDistance, finalValue, 1e-6f);
    }

    @Test
    public void getKernelMultiplier() {
        RolloutPolicy_DecayingHorizon.kernelCenter = 0.83f;
        RolloutPolicy_DecayingHorizon.kernelSteepness = 1.82f;
        RolloutPolicy_DecayingHorizon rollout =
                new RolloutPolicy_DecayingHorizon(new EvaluationFunction_Constant(1f), new Controller_Random());

        // Spot checks in MATLAB.
        Assert.assertEquals(0.953522734991563f, rollout.getKernelMultiplier(0f), 1e-8f);
        Assert.assertEquals(0.350054421864881f, rollout.getKernelMultiplier(1f), 1e-8f);
        Assert.assertEquals(0.922384856859664f, rollout.getKernelMultiplier(0.15f), 1e-8f);
    }

    // Fake version of the game which only returns one state.
    private static class NoFailGame extends GameUnified {
        @Override
        public boolean getFailureStatus() {
            return false;
        }
    }

    // Save the state half-way through for one of the tests.
    private static class CacheHalfwayPoint extends GameUnified {

        private boolean canFail;
        private int totalTs;
        IState halfwayCachedState;
        public CacheHalfwayPoint(boolean canFail, int totalTs) {
            this.totalTs = totalTs;
            this.canFail = canFail;
        }

        @Override
        public IState getCurrentState() {
            if (getTimestepsThisGame() == (totalTs / 2)) {
                halfwayCachedState = super.getCurrentState();
            }
            return super.getCurrentState();
        }

        @Override
        public boolean getFailureStatus() {
            if (canFail) {
                return super.getFailureStatus();
            } else {
                return false;
            }
        }
    }
}