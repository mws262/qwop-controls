package game;

import data.LoadStateStatistics;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Combined with testing for StateDelayEmbedded
public class GameUnifiedCachingTest {

    @Test
    public void test_stateCorrectness() {
        for (int i = 1; i < 10; i++) {
            for (int j = 1; j < 4; j++) {
                checkSpecificDelayEmbedding(i,j);
            }
        }
    }

    private void checkSpecificDelayEmbedding(int delay, int numDelayedStates) {
        float tol = 1e-6f;
        GameUnifiedCaching gameCache = new GameUnifiedCaching(delay, numDelayedStates);
        Assert.assertEquals((numDelayedStates + 1) * 36, gameCache.getStateDimension());
        GameUnified gameBasic = new GameUnified();

        State initState = (State) GameUnified.getInitialState();
        float[] initStateFlatPositions = initState.extractPositions(initState.getCenterX());

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

    @Test
    public void finiteDifferenceStates() {
        float[] f1 = {0.24f, 0.44f, 0.74f, 0.56f, 0.44f, 0.60f, 0.18f, 0.81f, 0.82f, 0.49f, 0.76f, 0.40f, 0.73f, 0.15f, 0.51f, 0.67f, 0.15f, 0.06f, 0.34f, 0.45f, 0.76f, 0.13f, 0.90f, 0.22f, 0.38f, 0.42f, 0.97f, 0.18f, 0.88f, 0.55f, 0.32f, 0.90f, 0.54f, 0.46f, 0.81f, 0.36f, 0.97f, 0.56f, 0.77f, 0.68f, 0.21f, 0.83f, 0.87f, 0.04f, 0.61f, 0.27f, 0.75f, 0.49f, 0.79f, 0.52f, 0.60f, 0.22f, 0.36f, 0.30f, 0.85f, 0.97f, 0.62f, 0.89f, 0.98f, 0.65f, 0.80f, 0.40f, 0.48f, 0.06f, 0.80f, 0.35f, 0.85f, 0.99f, 0.48f, 0.36f, 0.05f, 0.91f};

        float[] f2 = {0.70f, 0.44f, 0.97f, 0.72f, 0.66f, 0.51f, 0.88f, 0.06f, 0.47f, 0.31f, 0.19f, 0.15f, 0.90f,
                0.07f, 0.96f, 0.70f, 0.01f, 0.00f, 0.85f, 0.47f, 0.84f, 0.22f, 0.81f, 0.25f, 1.00f, 0.94f, 0.55f,
                0.91f, 0.64f, 0.78f, 0.57f, 0.19f, 0.91f, 0.51f, 1.00f, 0.38f, 0.21f, 0.73f, 0.31f, 0.12f, 0.89f, 0.69f, 0.94f, 0.42f, 0.82f, 0.80f, 0.36f, 0.91f, 0.29f, 0.17f, 0.46f, 0.24f, 0.00f, 0.62f, 0.66f, 0.66f, 0.68f, 0.85f, 0.04f, 0.68f, 0.57f, 0.69f, 0.90f, 0.27f, 0.07f, 0.31f, 0.79f, 0.50f, 0.44f, 0.17f, 0.26f, 0.68f};

        float[] f3 = {0.82f, 0.71f, 0.58f, 0.99f, 0.46f, 0.49f, 0.27f, 0.66f, 0.41f, 0.30f, 0.61f, 0.79f, 0.97f,
                0.77f, 0.60f, 0.45f, 0.15f, 0.21f, 0.70f, 0.96f, 0.64f, 0.89f, 0.22f, 0.68f, 0.32f, 0.03f, 0.82f,
                0.43f, 0.09f, 0.94f, 0.39f, 0.59f, 0.24f, 0.65f, 0.37f, 0.48f, 0.40f, 0.33f, 0.27f, 0.77f, 0.24f, 0.96f, 0.92f, 0.27f, 0.22f, 0.25f, 0.25f, 0.24f, 0.43f, 0.83f, 0.62f, 0.19f, 0.81f, 0.99f, 0.15f, 0.85f, 0.24f, 0.42f, 0.32f, 0.43f, 0.33f, 0.10f, 0.67f, 0.05f, 0.20f, 0.50f, 0.28f, 0.42f, 0.60f, 0.05f, 0.89f, 0.86f};

        float[] f4 = {0.09f, 0.18f, 0.01f, 0.59f, 0.71f, 0.76f, 0.21f, 0.97f, 0.48f, 0.06f, 0.18f, 0.34f, 0.88f,
                0.04f, 0.36f, 0.64f, 0.83f, 0.04f, 0.79f, 0.05f, 0.30f, 0.27f, 0.91f, 0.61f, 0.50f, 0.74f, 0.11f,
                0.88f, 0.53f, 0.01f, 0.16f, 0.26f, 0.04f, 0.68f, 0.03f, 0.50f, 0.48f, 0.28f, 0.59f, 0.29f, 0.92f,
                0.01f, 0.56f, 0.37f, 0.37f, 0.02f, 0.69f, 0.55f, 0.77f, 0.79f, 0.25f, 0.20f, 0.37f, 0.72f, 0.13f, 0.43f, 0.02f, 0.07f, 0.71f, 0.80f, 0.91f, 0.91f, 0.63f, 0.13f, 0.06f, 0.78f, 0.74f, 0.63f, 0.73f, 0.20f, 0.79f, 0.97f};

        float[] f5 = {0.42f, 0.25f, 0.48f, 0.67f, 0.85f, 0.95f, 0.13f, 0.70f, 0.08f, 0.10f, 0.13f, 0.52f, 0.48f,
                0.45f, 0.80f, 0.53f, 0.67f, 0.48f, 0.33f, 0.26f, 0.97f, 0.26f, 0.79f, 0.50f, 0.32f, 0.62f, 0.01f,
                0.06f, 0.92f, 0.91f, 0.83f, 0.91f, 0.68f, 0.78f, 0.69f, 0.20f, 0.45f, 0.79f, 0.00f, 0.56f, 0.43f, 0.06f, 0.44f, 0.84f, 0.62f, 0.74f, 0.94f, 0.33f, 0.75f, 0.87f, 0.43f, 0.52f, 0.65f, 0.47f, 0.18f, 0.23f, 0.41f, 0.02f, 0.75f, 0.06f, 0.43f, 0.54f, 0.61f, 0.44f, 0.08f, 0.28f, 0.07f, 0.73f, 0.35f, 0.65f, 0.48f, 0.38f};

        float[] expected = {0.00f, 0.44f, 0.74f, -0.06f, 0.81f, 0.82f, 0.49f, 0.15f, 0.51f, 0.10f, 0.45f, 0.76f,
                0.14f, 0.42f, 0.97f, 0.08f, 0.90f, 0.54f, 0.73f, 0.56f, 0.77f, 0.63f, 0.04f, 0.61f, 0.55f, 0.52f,
                0.60f, 0.61f, 0.97f, 0.62f, 0.56f, 0.40f, 0.48f, 0.61f, 0.99f, 0.48f, 0.46f, 0.00f, 0.23f, 0.70f, -0.75f, -0.35f, 0.17f, -0.08f, 0.45f, 0.51f, 0.02f, 0.08f, 0.62f, 0.52f, -0.42f, 0.25f, -0.71f, 0.37f, -0.76f, 0.17f, -0.46f, 0.07f, 0.38f, 0.21f, -0.50f, -0.35f, -0.14f, -0.19f, -0.31f, 0.06f, -0.23f, 0.29f, 0.42f, -0.06f, -0.49f, -0.04f, -0.34f, 0.27f, -0.62f, -1.31f, 1.35f, 0.29f, -0.10f, 0.78f, -0.81f, -0.66f, 0.47f, -0.28f, -1.30f, -1.43f, 0.69f, -0.43f, 1.11f, -1.04f, 0.95f, -0.57f, 0.42f, -0.09f, -0.53f, -0.81f, 0.64f, 1.01f, 0.30f, -0.32f, 0.50f, -0.50f, -0.01f, -0.88f, -0.65f, -0.45f, 0.41f, 0.20f, -0.51f, -1.07f, 0.44f, 1.86f, -1.64f, -0.16f, -0.06f, -2.21f, 0.93f, 0.90f, -1.87f, 0.14f, 2.16f, 3.05f, -1.67f, 0.38f, -1.84f, 1.51f, -1.06f, 0.92f, -0.06f, -0.25f, 0.78f, 1.56f, -0.44f, -1.71f, -0.83f, 0.81f, -1.11f, 0.72f, 0.83f, 2.28f, 0.84f, 1.42f, -0.12f, -0.23f, 2.420000f, 2.470000f, 0.780000f, -2.430000f, 1.350000f, -0.440000f, -0.090000f, 4.780000f, -0.370000f, -1.690000f, 4.390000f, 1.010000f, -3.380000f, -5.500000f, 3.260000f, 0.570000f, 3.550000f, -1.140000f, 1.060000f, -0.710000f, -1.210000f, 0.830000f, -0.660000f, -2.210000f, -0.120000f, 2.530000f, 1.910000f, -1.230000f, 1.940000f, -0.330000f, -2.710000f, -4.860000f, -1.010000f, -3.520000f, -0.280000f, -0.250000f};

        State s1 = new State(f1, false);
        State s2 = new State(f2, false);
        State s3 = new State(f3, false);
        State s4 = new State(f4, false);
        State s5 = new State(f5, false);

        StateDelayEmbedded.useFiniteDifferences = true;
        StateDelayEmbedded fullState = new StateDelayEmbedded(new State[]{s1, s2, s3, s4, s5});
        float[] result = fullState.flattenState();
        StateDelayEmbedded.useFiniteDifferences = false;

        Assert.assertArrayEquals(expected, result, 1e-5f);
    }

    @Test
    public void flattenStateWithRescaling() {
        // Fake means and standard deviations to test against.
        float[] stdev1 = new float[72];
        for (int i = 71; i >= 0; i--) {
            stdev1[i] = i;
        }
        State stdevState1 = new State(stdev1, false);

        float[] mean1 = new float[72];
        for (int i = 0; i < 72; i++) {
            mean1[i] = i;
        }
        State meanState1 = new State(mean1, false);

        LoadStateStatistics.StateStatistics stateStats = mock(LoadStateStatistics.StateStatistics.class);
        when(stateStats.getStdev()).thenReturn(stdevState1);
        when(stateStats.getMean()).thenReturn(meanState1);

        GameUnifiedCaching gameCache = new GameUnifiedCaching(1, 3);
        IState st = gameCache.getCurrentState();
        IState init = GameUnified.getInitialState();
        float[] initFlat = init.flattenState();

        float[] rescaled = st.flattenStateWithRescaling(stateStats);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 36; j++) {
                int fullIdx = j + (j/3) * 3; // Different since these are positions and velocities while others are
                // just positions.
                Assert.assertEquals((initFlat[fullIdx] - mean1[fullIdx]) / (stdev1[fullIdx] == 0 ? 1 : stdev1[fullIdx]),
                rescaled[36 * i + j], 1e-6f);

            }
        }
    }
}