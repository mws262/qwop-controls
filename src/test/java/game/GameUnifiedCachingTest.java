package game;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class GameUnifiedCachingTest {

    @Test
    public void test_stateCorrectness() {
        int delay = 1;
        int numDelayedStates = 3;
        GameUnifiedCaching gameCache = new GameUnifiedCaching(delay, numDelayedStates);
        GameUnified gameBasic = new GameUnified();

        IState initState = GameUnified.getInitialState();
        float[] initStateFlatPositions = extractPositionsFlat(initState.flattenState());

        IState s = gameCache.getCurrentState();
        Assert.assertEquals(s.getCenterX(), initState.getCenterX(), 1e-12f);
        Assert.assertEquals(12 * (numDelayedStates + 1), s.getStateVariableCount());

        float[] sflat = s.flattenState();
        for (int i = 0; i < numDelayedStates + 1; i++) {
            float[] sflatSection = Arrays.copyOfRange(sflat, i * 36, 36 * (i + 1));
            Assert.assertArrayEquals(initStateFlatPositions, sflatSection, 1e-12f);
        }

        //////////////////

        boolean[] c = new boolean[]{false, true, true, false};
        gameBasic.step(c);
        gameCache.step(c);

        IState stateBasic = gameBasic.getCurrentState();
        IState stateCache = gameCache.getCurrentState();
        float[] flatStateBasic = extractPositionsFlat(stateBasic.flattenState());
        float[] flatStateCache = stateCache.flattenState();

        float[] sflatSection = Arrays.copyOfRange(flatStateCache, 0, 36);
        Assert.assertArrayEquals(flatStateBasic, sflatSection, 1e-12f);

        sflatSection = Arrays.copyOfRange(flatStateCache, 36, 72);
        Assert.assertArrayEquals(initStateFlatPositions, initStateFlatPositions, 1e-12f);

        sflatSection = Arrays.copyOfRange(flatStateCache, 72, 108);
        Assert.assertArrayEquals(initStateFlatPositions, initStateFlatPositions, 1e-12f);

        sflatSection = Arrays.copyOfRange(flatStateCache, 72, 144);
        Assert.assertArrayEquals(initStateFlatPositions, initStateFlatPositions, 1e-12f);
    }

    private static float[] extractPositionsFlat(float[] fullstate) {
        float[] justPositions = new float[36];

        // Isolate just the positions.
        for (int i = 0; i < 36; i += 3) {
            justPositions[i] = fullstate[2 * i];
            justPositions[i + 1] = fullstate[2 * i + 1];
            justPositions[i + 2] = fullstate[2 * i + 2];
        }

        return justPositions;
    }
}