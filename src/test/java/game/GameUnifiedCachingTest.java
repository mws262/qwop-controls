package game;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class GameUnifiedCachingTest {

    @Test
    public void test_stateCorrectness() {
        for (int i = 1; i < 10; i++) {
            for (int j = 0; j < 4; j++) {
                runTests(i,j);
            }
        }
    }

    private void runTests(int delay, int numDelayedStates) {
        float tol = 1e-6f;
        GameUnifiedCaching gameCache = new GameUnifiedCaching(delay, numDelayedStates);
        GameUnified gameBasic = new GameUnified();

        IState initState = GameUnified.getInitialState();
        float[] initStateFlatPositions = extractPositionsFlat(initState.flattenState(), 0);

        // Test initial state. All cached states should be initial positions.
        IState s = gameCache.getCurrentState();
        Assert.assertEquals(s.getCenterX(), initState.getCenterX(), tol);
        Assert.assertEquals(12 * (numDelayedStates + 1), s.getStateVariableCount());

        float[] sflat = s.flattenState();
        for (int i = 0; i < numDelayedStates + 1; i++) {
            float[] sflatSection = Arrays.copyOfRange(sflat, i * 36, 36 * (i + 1));
            Assert.assertArrayEquals(initStateFlatPositions, sflatSection, tol);
        }

        // After 1 timestep. First positions should be updated, others should be initial positions.
        boolean[] c = new boolean[]{false, true, true, false};
        for (int i = 0; i < delay; i++) {
            gameBasic.step(c);
            gameCache.step(c);
        }

        IState stateBasic1 = gameBasic.getCurrentState();
        IState stateCache1 = gameCache.getCurrentState();
        float[] flatStateCache = stateCache1.flattenState();
        float[] flatStateBasic = extractPositionsFlat(stateBasic1.flattenState(), 0);

        float[] sflatSection = Arrays.copyOfRange(flatStateCache, 0, 36);
        Assert.assertArrayEquals(flatStateBasic, sflatSection, tol);

        if (numDelayedStates > 0) {
            float[] flatStatePrev1 = extractPositionsFlat(initState.flattenState(), flatStateCache[36]);
            sflatSection = Arrays.copyOfRange(flatStateCache, 36, 72);
            Assert.assertArrayEquals(flatStatePrev1, sflatSection, tol);

            if (numDelayedStates > 1) {
                sflatSection = Arrays.copyOfRange(flatStateCache, 72, 108);
                Assert.assertArrayEquals(flatStatePrev1, sflatSection, tol);

                if (numDelayedStates > 2) {
                    sflatSection = Arrays.copyOfRange(flatStateCache, 108, 144);
                    Assert.assertArrayEquals(flatStatePrev1, sflatSection, tol);
                }
            }
        }
        // After 2 timesteps.
        c = new boolean[]{false, true, true, false};
        for (int i = 0; i < delay; i++) {
            gameBasic.step(c);
            gameCache.step(c);
        }

        IState stateBasic2 = gameBasic.getCurrentState();
        IState stateCache2 = gameCache.getCurrentState();
        flatStateCache = stateCache2.flattenState();
        flatStateBasic = extractPositionsFlat(stateBasic2.flattenState(), 0);

        sflatSection = Arrays.copyOfRange(flatStateCache, 0, 36);
        Assert.assertArrayEquals(flatStateBasic, sflatSection, tol);

        if (numDelayedStates > 0) {
            float[] flatStatePrev1 = extractPositionsFlat(stateBasic1.flattenState(), flatStateCache[36]);

            sflatSection = Arrays.copyOfRange(flatStateCache, 36, 72);
            Assert.assertArrayEquals(flatStatePrev1, sflatSection, tol);

            if (numDelayedStates > 1) {
                float[] flatStatePrev2 = extractPositionsFlat(initState.flattenState(), flatStateCache[72]);
                sflatSection = Arrays.copyOfRange(flatStateCache, 72, 108);
                Assert.assertArrayEquals(flatStatePrev2, sflatSection, tol);

                if (numDelayedStates > 2) {
                    sflatSection = Arrays.copyOfRange(flatStateCache, 108, 144);
                    Assert.assertArrayEquals(flatStatePrev2, sflatSection, tol);
                }
            }
        }

        // After 3 timesteps.
        c = new boolean[]{false, true, true, false};
        for (int i = 0; i < delay; i++) {
            gameBasic.step(c);
            gameCache.step(c);
        }

        IState stateBasic3 = gameBasic.getCurrentState();
        IState stateCache3 = gameCache.getCurrentState();
        flatStateCache = stateCache3.flattenState();
        flatStateBasic = extractPositionsFlat(stateBasic3.flattenState(), 0);

        sflatSection = Arrays.copyOfRange(flatStateCache, 0, 36);
        Assert.assertArrayEquals(flatStateBasic, sflatSection, tol);

        if (numDelayedStates > 0) {
            float[] flatStatePrev1 = extractPositionsFlat(stateBasic2.flattenState(), flatStateCache[36]);

            sflatSection = Arrays.copyOfRange(flatStateCache, 36, 72);
            Assert.assertArrayEquals(flatStatePrev1, sflatSection, tol);

            if (numDelayedStates > 1) {
                float[] flatStatePrev2 = extractPositionsFlat(stateBasic1.flattenState(), flatStateCache[72]);
                sflatSection = Arrays.copyOfRange(flatStateCache, 72, 108);
                Assert.assertArrayEquals(flatStatePrev2, sflatSection, tol);

                if (numDelayedStates > 2) {
                    float[] flatStatePrev3 = extractPositionsFlat(initState.flattenState(), flatStateCache[108]);

                    sflatSection = Arrays.copyOfRange(flatStateCache, 108, 144);
                    Assert.assertArrayEquals(flatStatePrev3, sflatSection, tol);
                }
            }
        }
        // After 4 timesteps.
        c = new boolean[]{false, true, true, false};
        for (int i = 0; i < delay; i++) {
            gameBasic.step(c);
            gameCache.step(c);
        }

        IState stateBasic4 = gameBasic.getCurrentState();
        IState stateCache4 = gameCache.getCurrentState();
        flatStateCache = stateCache4.flattenState();
        flatStateBasic = extractPositionsFlat(stateBasic4.flattenState(), 0);

        sflatSection = Arrays.copyOfRange(flatStateCache, 0, 36);
        Assert.assertArrayEquals(flatStateBasic, sflatSection, tol);

        if (numDelayedStates > 0) {
            float[] flatStatePrev1 = extractPositionsFlat(stateBasic3.flattenState(), flatStateCache[36]);
            sflatSection = Arrays.copyOfRange(flatStateCache, 36, 72);
            Assert.assertArrayEquals(flatStatePrev1, sflatSection, tol);

            if (numDelayedStates > 1) {
                float[] flatStatePrev2 = extractPositionsFlat(stateBasic2.flattenState(), flatStateCache[72]);

                sflatSection = Arrays.copyOfRange(flatStateCache, 72, 108);
                Assert.assertArrayEquals(flatStatePrev2, sflatSection, tol);

                if (numDelayedStates > 2) {
                    float[] flatStatePrev3 = extractPositionsFlat(stateBasic1.flattenState(), flatStateCache[108]);

                    sflatSection = Arrays.copyOfRange(flatStateCache, 108, 144);
                    Assert.assertArrayEquals(flatStatePrev3, sflatSection, tol);
                }
            }
        }
        // After 5 timesteps.
        c = new boolean[]{false, true, true, false};
        for (int i = 0; i < delay; i++) {
            gameBasic.step(c);
            gameCache.step(c);
        }

        IState stateBasic5 = gameBasic.getCurrentState();
        IState stateCache5 = gameCache.getCurrentState();
        flatStateCache = stateCache5.flattenState();
        flatStateBasic = extractPositionsFlat(stateBasic5.flattenState(), 0);

        sflatSection = Arrays.copyOfRange(flatStateCache, 0, 36);
        Assert.assertArrayEquals(flatStateBasic, sflatSection, tol);

        if (numDelayedStates > 0) {
            float[] flatStatePrev1 = extractPositionsFlat(stateBasic4.flattenState(), flatStateCache[36]);

            sflatSection = Arrays.copyOfRange(flatStateCache, 36, 72);
            Assert.assertArrayEquals(flatStatePrev1, sflatSection, tol);

            if (numDelayedStates > 1) {
                float[] flatStatePrev2 = extractPositionsFlat(stateBasic3.flattenState(), flatStateCache[72]);

                sflatSection = Arrays.copyOfRange(flatStateCache, 72, 108);
                Assert.assertArrayEquals(flatStatePrev2, sflatSection, tol);

                if (numDelayedStates > 2) {
                    float[] flatStatePrev3 = extractPositionsFlat(stateBasic2.flattenState(), flatStateCache[108]);

                    sflatSection = Arrays.copyOfRange(flatStateCache, 108, 144);
                    Assert.assertArrayEquals(flatStatePrev3, sflatSection, tol);
                }
            }
        }
    }

    private static float[] extractPositionsFlat(float[] fullstate, float xOffset) {
        float[] justPositions = new float[36];

        // Isolate just the positions.
        for (int i = 0; i < 36; i += 3) {
            justPositions[i] = fullstate[2 * i] + xOffset;
            justPositions[i + 1] = fullstate[2 * i + 1];
            justPositions[i + 2] = fullstate[2 * i + 2];
        }

        return justPositions;
    }
}