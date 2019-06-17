package game;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class StateTest {

    @Test
    public void add() {
        float[] f1 = new float[]{0.66f, 0.57f, 0.40f, 0.93f, 0.36f, 0.95f, 0.03f, 0.44f, 0.70f, 0.70f, 0.51f, 0.61f,
                0.75f, 0.80f, 0.34f, 0.56f, 0.86f, 0.59f, 0.51f, 0.85f, 0.68f, 0.11f, 0.50f, 0.02f, 0.44f, 0.91f, 0.04f, 0.95f, 0.18f, 0.37f, 0.21f, 0.10f, 0.43f, 0.28f, 0.10f, 0.50f, 0.24f, 0.35f, 0.56f, 0.55f, 0.42f, 0.39f, 0.38f, 0.28f, 0.18f, 0.13f, 0.02f, 0.75f, 0.50f, 0.72f, 0.76f, 0.27f, 0.19f, 0.06f, 0.43f, 0.70f, 0.98f, 0.55f, 0.04f, 0.58f, 0.28f, 0.54f, 0.44f, 0.12f, 0.38f, 0.76f, 0.92f, 0.69f, 0.97f, 0.13f, 0.65f, 0.39f};
        float[] f2 = new float[]{0.56f, 0.20f, 0.39f, 0.34f, 0.36f, 0.37f, 0.12f, 0.50f, 0.82f, 0.34f, 0.50f, 0.12f,
                0.58f, 0.52f, 0.36f, 0.86f, 0.34f, 0.46f, 0.24f, 0.77f, 0.27f, 0.53f, 0.53f, 0.87f, 0.94f, 0.05f, 0.98f, 0.63f, 0.07f, 0.21f, 0.36f, 0.43f, 0.76f, 0.92f, 0.63f, 0.96f, 0.76f, 0.53f, 0.85f, 0.82f, 0.13f, 0.76f, 0.62f, 0.40f, 0.84f, 0.04f, 0.10f, 0.95f, 0.40f, 0.53f, 0.90f, 0.34f, 0.66f, 0.73f, 0.22f, 0.56f, 0.46f, 0.61f, 0.54f, 0.52f, 0.29f, 0.88f, 0.94f, 0.81f, 0.97f, 0.52f, 0.11f, 0.65f, 0.19f, 0.86f, 0.94f, 0.10f};
        float[] expected = new float[] {1.22f, 0.77f, 0.79f, 1.27f, 0.72f, 1.32f, 0.15f, 0.94f, 1.52f, 1.04f, 1.01f,
                0.73f, 1.33f, 1.32f, 0.70f, 1.42f, 1.20f, 1.05f, 0.75f, 1.62f, 0.95f, 0.64f, 1.03f, 0.89f, 1.38f, 0.96f, 1.02f, 1.58f, 0.25f, 0.58f, 0.57f, 0.53f, 1.19f, 1.20f, 0.73f, 1.46f, 1.00f, 0.88f, 1.41f, 1.37f, 0.55f, 1.15f, 1.00f, 0.68f, 1.02f, 0.17f, 0.12f, 1.70f, 0.90f, 1.25f, 1.66f, 0.61f, 0.85f, 0.79f, 0.65f, 1.26f, 1.44f, 1.16f, 0.58f, 1.10f, 0.57f, 1.42f, 1.38f, 0.93f, 1.35f, 1.28f, 1.03f, 1.34f, 1.16f, 0.99f, 1.59f, 0.49f};
        State s1 = new State(f1, false);
        State s2 = new State(f2, false);

        State result = s1.add(s2);
        Assert.assertArrayEquals(expected, result.flattenState(0), 1e-6f);

    }

    @Test
    public void subtract() {
    }

    @Test
    public void divide() {
    }

    @Test
    public void multiply() {
    }

    @Test
    public void extractPositions() {
    }

    @Test
    public void extractPositions1() {
    }

    @Test
    public void xOffsetState() {
    }
}