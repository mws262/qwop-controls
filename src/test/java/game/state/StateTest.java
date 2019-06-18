package game.state;

import game.state.IState.ObjectName;
import game.state.State;
import game.state.StateVariable;
import org.junit.Assert;
import org.junit.Test;

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
        float[] f1 = new float[]{0.36f, 0.53f, 0.71f, 0.87f, 0.33f, 0.65f, 0.97f, 0.08f, 0.59f, 0.41f, 0.31f, 0.26f,
                0.76f, 1.00f, 0.19f, 0.78f, 0.20f, 0.99f, 0.80f, 0.42f, 0.73f, 0.50f, 0.81f, 0.36f, 0.07f, 0.59f,
                0.91f, 0.19f, 0.43f, 0.75f, 0.04f, 0.95f, 0.76f, 0.56f, 0.18f, 0.50f, 0.52f, 0.99f, 0.85f, 0.96f, 0.68f, 0.40f, 0.93f, 0.48f, 0.23f, 0.40f, 0.71f, 0.56f, 0.76f, 1.00f, 0.96f, 0.54f, 0.96f, 0.12f, 0.05f, 0.30f, 0.58f, 0.53f, 0.90f, 0.54f, 0.43f, 0.54f, 0.71f, 0.02f, 0.80f, 0.14f, 0.48f, 0.26f, 0.37f, 0.66f, 0.17f, 0.28f};
        float[] f2 = new float[]{0.20f, 0.20f, 0.33f, 0.88f, 0.47f, 0.40f, 0.18f, 0.97f, 0.41f, 0.84f, 0.62f, 0.38f,
                0.88f, 0.78f, 0.46f, 0.81f, 0.90f, 0.43f, 0.33f, 0.60f, 0.90f, 0.70f, 0.38f, 0.73f, 0.95f, 0.54f,
                0.54f, 0.31f, 0.07f, 0.18f, 0.09f, 0.46f, 0.01f, 0.92f, 0.64f, 0.00f, 0.03f, 0.21f, 0.45f, 0.13f, 0.01f, 0.73f, 0.35f, 0.78f, 0.44f, 0.44f, 0.05f, 0.05f, 0.09f, 0.59f, 0.24f, 0.84f, 0.86f, 0.96f, 0.49f, 0.22f, 0.23f, 0.54f, 0.76f, 0.35f, 0.46f, 0.64f, 0.92f, 0.16f, 0.72f, 0.58f, 0.43f, 0.88f, 0.39f, 0.18f, 0.63f, 0.62f};
        float[] expected = new float[] {0.16f, 0.33f, 0.38f, -0.01f, -0.14f, 0.25f, 0.79f, -0.89f, 0.18f, -0.43f,
                -0.31f, -0.12f, -0.12f, 0.22f, -0.27f, -0.03f, -0.70f, 0.56f, 0.47f, -0.18f, -0.17f, -0.20f, 0.43f,
                -0.37f, -0.88f, 0.05f, 0.37f, -0.12f, 0.36f, 0.57f, -0.05f, 0.49f, 0.75f, -0.36f, -0.46f, 0.50f, 0.49f, 0.78f, 0.40f, 0.83f, 0.67f, -0.33f, 0.58f, -0.30f, -0.21f, -0.04f, 0.66f, 0.51f, 0.67f, 0.41f, 0.72f, -0.30f, 0.10f, -0.84f, -0.44f, 0.08f, 0.35f, -0.01f, 0.14f, 0.19f, -0.03f, -0.10f, -0.21f, -0.14f, 0.08f, -0.44f, 0.05f, -0.62f, -0.02f, 0.48f, -0.46f, -0.34f};

        State s1 = new State(f1, false);
        State s2 = new State(f2, false);

        State result = s1.subtract(s2);
        Assert.assertArrayEquals(expected, result.flattenState(0), 1e-6f);
    }

    @Test
    public void multiply() {
        float[] f1 = new float[]{0.36f, 0.53f, 0.71f, 0.87f, 0.33f, 0.65f, 0.97f, 0.08f, 0.59f, 0.41f, 0.31f, 0.26f,
                0.76f, 1.00f, 0.19f, 0.78f, 0.20f, 0.99f, 0.80f, 0.42f, 0.73f, 0.50f, 0.81f, 0.36f, 0.07f, 0.59f,
                0.91f, 0.19f, 0.43f, 0.75f, 0.04f, 0.95f, 0.76f, 0.56f, 0.18f, 0.50f, 0.52f, 0.99f, 0.85f, 0.96f, 0.68f, 0.40f, 0.93f, 0.48f, 0.23f, 0.40f, 0.71f, 0.56f, 0.76f, 1.00f, 0.96f, 0.54f, 0.96f, 0.12f, 0.05f, 0.30f, 0.58f, 0.53f, 0.90f, 0.54f, 0.43f, 0.54f, 0.71f, 0.02f, 0.80f, 0.14f, 0.48f, 0.26f, 0.37f, 0.66f, 0.17f, 0.28f};
        float[] f2 = new float[]{0.20f, 0.20f, 0.33f, 0.88f, 0.47f, 0.40f, 0.18f, 0.97f, 0.41f, 0.84f, 0.62f, 0.38f,
                0.88f, 0.78f, 0.46f, 0.81f, 0.90f, 0.43f, 0.33f, 0.60f, 0.90f, 0.70f, 0.38f, 0.73f, 0.95f, 0.54f,
                0.54f, 0.31f, 0.07f, 0.18f, 0.09f, 0.46f, 0.01f, 0.92f, 0.64f, 0.00f, 0.03f, 0.21f, 0.45f, 0.13f, 0.01f, 0.73f, 0.35f, 0.78f, 0.44f, 0.44f, 0.05f, 0.05f, 0.09f, 0.59f, 0.24f, 0.84f, 0.86f, 0.96f, 0.49f, 0.22f, 0.23f, 0.54f, 0.76f, 0.35f, 0.46f, 0.64f, 0.92f, 0.16f, 0.72f, 0.58f, 0.43f, 0.88f, 0.39f, 0.18f, 0.63f, 0.62f};
        float[] expected = new float[] {0.072000f, 0.106000f, 0.234300f, 0.765600f, 0.155100f, 0.260000f, 0.174600f,
                0.077600f, 0.241900f, 0.344400f, 0.192200f, 0.098800f, 0.668800f, 0.780000f, 0.087400f, 0.631800f,
                0.180000f, 0.425700f, 0.264000f, 0.252000f, 0.657000f, 0.350000f, 0.307800f, 0.262800f, 0.066500f, 0.318600f, 0.491400f, 0.058900f, 0.030100f, 0.135000f, 0.003600f, 0.437000f, 0.007600f, 0.515200f, 0.115200f, 0.000000f, 0.015600f, 0.207900f, 0.382500f, 0.124800f, 0.006800f, 0.292000f, 0.325500f, 0.374400f, 0.101200f, 0.176000f, 0.035500f, 0.028000f, 0.068400f, 0.590000f, 0.230400f, 0.453600f, 0.825600f, 0.115200f, 0.024500f, 0.066000f, 0.133400f, 0.286200f, 0.684000f, 0.189000f, 0.197800f, 0.345600f, 0.653200f, 0.003200f, 0.576000f, 0.081200f, 0.206400f, 0.228800f, 0.144300f, 0.118800f, 0.107100f, 0.173600f};

                State s1 = new State(f1, false);
        State s2 = new State(f2, false);

        State result = s1.multiply(s2);
        Assert.assertArrayEquals(expected, result.flattenState(0), 1e-6f);
    }

    @Test
    public void divide() {
        float[] f1 = new float[]{0.36f, 0.53f, 0.71f, 0.87f, 0.33f, 0.65f, 0.97f, 0.08f, 0.59f, 0.41f, 0.31f, 0.26f,
                0.76f, 1.00f, 0.19f, 0.78f, 0.20f, 0.99f, 0.80f, 0.42f, 0.73f, 0.50f, 0.81f, 0.36f, 0.07f, 0.59f,
                0.91f, 0.19f, 0.43f, 0.75f, 0.04f, 0.95f, 0.76f, 0.56f, 0.18f, 0.50f, 0.52f, 0.99f, 0.85f, 0.96f, 0.68f, 0.40f, 0.93f, 0.48f, 0.23f, 0.40f, 0.71f, 0.56f, 0.76f, 1.00f, 0.96f, 0.54f, 0.96f, 0.12f, 0.05f, 0.30f, 0.58f, 0.53f, 0.90f, 0.54f, 0.43f, 0.54f, 0.71f, 0.02f, 0.80f, 0.14f, 0.48f, 0.26f, 0.37f, 0.66f, 0.17f, 0.28f};
        float[] f2 = new float[]{0.20f, 0.20f, 0.33f, 0.88f, 0.47f, 0.40f, 0.18f, 0.97f, 0.41f, 0.84f, 0.62f, 0.38f,
                0.88f, 0.78f, 0.46f, 0.81f, 0.90f, 0.43f, 0.33f, 0.60f, 0.90f, 0.70f, 0.38f, 0.73f, 0.95f, 0.54f,
                0.54f, 0.31f, 0.07f, 0.18f, 0.09f, 0.46f, 0.01f, 0.92f, 0.64f, 0.00f, 0.03f, 0.21f, 0.45f, 0.13f, 0.01f, 0.73f, 0.35f, 0.78f, 0.44f, 0.44f, 0.05f, 0.05f, 0.09f, 0.59f, 0.24f, 0.84f, 0.86f, 0.96f, 0.49f, 0.22f, 0.23f, 0.54f, 0.76f, 0.35f, 0.46f, 0.64f, 0.92f, 0.16f, 0.72f, 0.58f, 0.43f, 0.88f, 0.39f, 0.18f, 0.63f, 0.62f};
        // The expected values are the precision-limiting factor. I didn't use format long when calculating in MATLAB.
        float[] expected = new float[] {1.800000f, 2.650000f, 2.151515f, 0.988636f, 0.702128f, 1.625000f, 5.388889f,
                0.082474f, 1.439024f, 0.488095f, 0.500000f, 0.684211f, 0.863636f, 1.282051f, 0.413043f, 0.962963f,
                0.222222f, 2.302326f, 2.424242f, 0.700000f, 0.811111f, 0.714286f, 2.131579f, 0.493151f, 0.073684f,
                1.092593f, 1.685185f, 0.612903f, 6.142857f, 4.166667f, 0.444444f, 2.065217f, 76.000000f, 0.608696f,
                0.281250f, 0.5f, 17.333333f, 4.714286f, 1.888889f, 7.384615f, 68.000000f, 0.547945f, 2.657143f,
                0.615385f, 0.522727f, 0.909091f, 14.200000f, 11.200000f, 8.444444f, 1.694915f, 4.000000f, 0.642857f, 1.116279f, 0.125000f, 0.102041f, 1.363636f, 2.521739f, 0.981481f, 1.184211f, 1.542857f, 0.934783f, 0.843750f, 0.771739f, 0.125000f, 1.111111f, 0.241379f, 1.116279f, 0.295455f, 0.948718f, 3.666667f, 0.269841f, 0.451613f};

        State s1 = new State(f1, false);
        State s2 = new State(f2, false);

        State result = s1.divide(s2);
        Assert.assertArrayEquals(expected, result.flattenState(0), 1e-5f);
    }

    @Test
    public void extractPositions() {
        float[] f = new float[]{0.05f, 0.21f, 0.40f, 0.33f, 0.23f, 0.94f, 0.68f, 0.96f, 0.44f, 0.94f, 0.01f, 0.61f,
                0.80f, 0.23f, 0.93f, 0.76f, 0.83f, 0.57f, 0.79f, 0.33f, 0.22f, 0.31f, 0.58f, 0.83f, 0.29f, 0.40f, 0.86f, 0.61f, 0.99f, 0.20f, 0.83f, 0.68f, 0.25f, 0.48f, 0.40f, 0.60f, 0.80f, 0.11f, 0.82f, 0.84f, 0.35f, 0.43f, 0.57f, 0.70f, 0.74f, 0.76f, 0.39f, 0.43f, 0.96f, 0.57f, 0.85f, 0.28f, 0.62f, 0.59f, 0.96f, 0.09f, 0.50f, 0.52f, 0.09f, 0.90f, 0.88f, 0.44f, 0.78f, 0.15f, 0.62f, 0.26f, 0.45f, 0.84f, 0.20f, 0.30f, 0.48f, 0.34f};
        State s = new State(f, false);


        float[] fpos = s.extractPositions(0f);
        float[] zeroOffsetExpected = new float[]{0.05f, 0.21f, 0.40f, 0.68f, 0.96f, 0.44f, 0.80f, 0.23f, 0.93f, 0.79f
                , 0.33f, 0.22f, 0.29f, 0.40f, 0.86f, 0.83f, 0.68f, 0.25f, 0.80f, 0.11f, 0.82f, 0.57f, 0.70f, 0.74f, 0.96f, 0.57f, 0.85f, 0.96f, 0.09f, 0.50f, 0.88f, 0.44f, 0.78f, 0.45f, 0.84f, 0.20f};
        Assert.assertArrayEquals(zeroOffsetExpected, fpos, 1e-6f);

        fpos = s.extractPositions();
        Assert.assertArrayEquals(zeroOffsetExpected, fpos, 1e-6f);

        fpos = s.extractPositions(-1f);
        float[] oneOffsetExpected = new float[]{1.05f, 0.21f, 0.40f, 1.68f, 0.96f, 0.44f, 1.80f, 0.23f, 0.93f, 1.79f,
                0.33f, 0.22f, 1.29f, 0.40f, 0.86f, 1.83f, 0.68f, 0.25f, 1.80f, 0.11f, 0.82f, 1.57f, 0.70f, 0.74f, 1.96f, 0.57f, 0.85f, 1.96f, 0.09f, 0.50f, 1.88f, 0.44f, 0.78f, 1.45f, 0.84f, 0.20f};
        Assert.assertArrayEquals(oneOffsetExpected, fpos, 1e-6f);

        fpos = s.extractPositions(0.05f);
        float[] offsetX = new float[] {0.00f, 0.21f, 0.40f, 0.63f, 0.96f, 0.44f, 0.75f, 0.23f, 0.93f, 0.74f, 0.33f,
                0.22f, 0.24f, 0.40f, 0.86f, 0.78f, 0.68f, 0.25f, 0.75f, 0.11f, 0.82f, 0.52f, 0.70f, 0.74f, 0.91f, 0.57f, 0.85f, 0.91f, 0.09f, 0.50f, 0.83f, 0.44f, 0.78f, 0.40f, 0.84f, 0.20f};
        Assert.assertArrayEquals(offsetX, fpos, 1e-6f);

    }

    @Test
    public void flattenState() {
        float[] f = new float[]{0.05f, 0.21f, 0.40f, 0.33f, 0.23f, 0.94f, 0.68f, 0.96f, 0.44f, 0.94f, 0.01f, 0.61f,
                0.80f, 0.23f, 0.93f, 0.76f, 0.83f, 0.57f, 0.79f, 0.33f, 0.22f, 0.31f, 0.58f, 0.83f, 0.29f, 0.40f, 0.86f, 0.61f, 0.99f, 0.20f, 0.83f, 0.68f, 0.25f, 0.48f, 0.40f, 0.60f, 0.80f, 0.11f, 0.82f, 0.84f, 0.35f, 0.43f, 0.57f, 0.70f, 0.74f, 0.76f, 0.39f, 0.43f, 0.96f, 0.57f, 0.85f, 0.28f, 0.62f, 0.59f, 0.96f, 0.09f, 0.50f, 0.52f, 0.09f, 0.90f, 0.88f, 0.44f, 0.78f, 0.15f, 0.62f, 0.26f, 0.45f, 0.84f, 0.20f, 0.30f, 0.48f, 0.34f};
        State s = new State(f, false);

        float[] expected = new float[] {0.00f, 0.21f, 0.40f, 0.33f, 0.23f, 0.94f, 0.63f, 0.96f, 0.44f, 0.94f, 0.01f,
                0.61f, 0.75f, 0.23f, 0.93f, 0.76f, 0.83f, 0.57f, 0.74f, 0.33f, 0.22f, 0.31f, 0.58f, 0.83f, 0.24f,
                0.40f, 0.86f, 0.61f, 0.99f, 0.20f, 0.78f, 0.68f, 0.25f, 0.48f, 0.40f, 0.60f, 0.75f, 0.11f, 0.82f, 0.84f, 0.35f, 0.43f, 0.52f, 0.70f, 0.74f, 0.76f, 0.39f, 0.43f, 0.91f, 0.57f, 0.85f, 0.28f, 0.62f, 0.59f, 0.91f, 0.09f, 0.50f, 0.52f, 0.09f, 0.90f, 0.83f, 0.44f, 0.78f, 0.15f, 0.62f, 0.26f, 0.40f, 0.84f, 0.20f, 0.30f, 0.48f, 0.34f};
        Assert.assertArrayEquals(expected, s.flattenState(), 1e-6f);

        Assert.assertArrayEquals(f, s.flattenState(0f), 1e-6f);
    }

    @Test
    public void xOffsetSubtract() {
        float[] f = new float[]{0.05f, 0.21f, 0.40f, 0.33f, 0.23f, 0.94f, 0.68f, 0.96f, 0.44f, 0.94f, 0.01f, 0.61f,
                0.80f, 0.23f, 0.93f, 0.76f, 0.83f, 0.57f, 0.79f, 0.33f, 0.22f, 0.31f, 0.58f, 0.83f, 0.29f, 0.40f, 0.86f, 0.61f, 0.99f, 0.20f, 0.83f, 0.68f, 0.25f, 0.48f, 0.40f, 0.60f, 0.80f, 0.11f, 0.82f, 0.84f, 0.35f, 0.43f, 0.57f, 0.70f, 0.74f, 0.76f, 0.39f, 0.43f, 0.96f, 0.57f, 0.85f, 0.28f, 0.62f, 0.59f, 0.96f, 0.09f, 0.50f, 0.52f, 0.09f, 0.90f, 0.88f, 0.44f, 0.78f, 0.15f, 0.62f, 0.26f, 0.45f, 0.84f, 0.20f, 0.30f, 0.48f, 0.34f};
        State s = new State(f, false);

        State offset = s.xOffsetSubtract(0.05f);
        float[] fOffset = offset.flattenState(0f);
        float[] expected = new float[] {0.00f, 0.21f, 0.40f, 0.33f, 0.23f, 0.94f, 0.63f, 0.96f, 0.44f, 0.94f, 0.01f,
                0.61f, 0.75f, 0.23f, 0.93f, 0.76f, 0.83f, 0.57f, 0.74f, 0.33f, 0.22f, 0.31f, 0.58f, 0.83f, 0.24f,
                0.40f, 0.86f, 0.61f, 0.99f, 0.20f, 0.78f, 0.68f, 0.25f, 0.48f, 0.40f, 0.60f, 0.75f, 0.11f, 0.82f, 0.84f, 0.35f, 0.43f, 0.52f, 0.70f, 0.74f, 0.76f, 0.39f, 0.43f, 0.91f, 0.57f, 0.85f, 0.28f, 0.62f, 0.59f, 0.91f, 0.09f, 0.50f, 0.52f, 0.09f, 0.90f, 0.83f, 0.44f, 0.78f, 0.15f, 0.62f, 0.26f, 0.40f, 0.84f, 0.20f, 0.30f, 0.48f, 0.34f};
        Assert.assertArrayEquals(expected, fOffset, 1e-6f);
    }

    @Test
    public void getStateVariableFromName() {
        float[] f = new float[]{0.05f, 0.21f, 0.40f, 0.33f, 0.23f, 0.94f, 0.68f, 0.96f, 0.44f, 0.94f, 0.01f, 0.61f,
                0.80f, 0.23f, 0.93f, 0.76f, 0.83f, 0.57f, 0.79f, 0.33f, 0.22f, 0.31f, 0.58f, 0.83f, 0.29f, 0.40f, 0.86f, 0.61f, 0.99f, 0.20f, 0.83f, 0.68f, 0.25f, 0.48f, 0.40f, 0.60f, 0.80f, 0.11f, 0.82f, 0.84f, 0.35f, 0.43f, 0.57f, 0.70f, 0.74f, 0.76f, 0.39f, 0.43f, 0.96f, 0.57f, 0.85f, 0.28f, 0.62f, 0.59f, 0.96f, 0.09f, 0.50f, 0.52f, 0.09f, 0.90f, 0.88f, 0.44f, 0.78f, 0.15f, 0.62f, 0.26f, 0.45f, 0.84f, 0.20f, 0.30f, 0.48f, 0.34f};
        State s = new State(f, false);

        int idx = 0;
        StateVariable sv = s.getStateVariableFromName(ObjectName.BODY);
        Assert.assertEquals(sv.getX(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getY(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getTh(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getDx(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getDy(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getDth(), f[idx++], 1e-12);

        sv = s.getStateVariableFromName(ObjectName.HEAD);
        Assert.assertEquals(sv.getX(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getY(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getTh(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getDx(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getDy(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getDth(), f[idx++], 1e-12);

        sv = s.getStateVariableFromName(ObjectName.RTHIGH);
        Assert.assertEquals(sv.getX(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getY(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getTh(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getDx(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getDy(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getDth(), f[idx++], 1e-12);

        sv = s.getStateVariableFromName(ObjectName.LTHIGH);
        Assert.assertEquals(sv.getX(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getY(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getTh(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getDx(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getDy(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getDth(), f[idx++], 1e-12);

        sv = s.getStateVariableFromName(ObjectName.RCALF);
        Assert.assertEquals(sv.getX(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getY(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getTh(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getDx(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getDy(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getDth(), f[idx++], 1e-12);

        sv = s.getStateVariableFromName(ObjectName.LCALF);
        Assert.assertEquals(sv.getX(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getY(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getTh(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getDx(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getDy(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getDth(), f[idx++], 1e-12);

        sv = s.getStateVariableFromName(ObjectName.RFOOT);
        Assert.assertEquals(sv.getX(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getY(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getTh(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getDx(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getDy(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getDth(), f[idx++], 1e-12);

        sv = s.getStateVariableFromName(ObjectName.LFOOT);
        Assert.assertEquals(sv.getX(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getY(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getTh(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getDx(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getDy(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getDth(), f[idx++], 1e-12);

        sv = s.getStateVariableFromName(ObjectName.RUARM);
        Assert.assertEquals(sv.getX(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getY(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getTh(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getDx(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getDy(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getDth(), f[idx++], 1e-12);

        sv = s.getStateVariableFromName(ObjectName.LUARM);
        Assert.assertEquals(sv.getX(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getY(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getTh(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getDx(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getDy(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getDth(), f[idx++], 1e-12);

        sv = s.getStateVariableFromName(ObjectName.RLARM);
        Assert.assertEquals(sv.getX(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getY(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getTh(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getDx(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getDy(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getDth(), f[idx++], 1e-12);

        sv = s.getStateVariableFromName(ObjectName.LLARM);
        Assert.assertEquals(sv.getX(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getY(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getTh(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getDx(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getDy(), f[idx++], 1e-12);
        Assert.assertEquals(sv.getDth(), f[idx++], 1e-12);

        // Make sure that this order is ok too.
        idx = 0;
        for (StateVariable sVar : s.getAllStateVariables()) {
            Assert.assertEquals(sVar.getX(), f[idx++], 1e-12);
            Assert.assertEquals(sVar.getY(), f[idx++], 1e-12);
            Assert.assertEquals(sVar.getTh(), f[idx++], 1e-12);
            Assert.assertEquals(sVar.getDx(), f[idx++], 1e-12);
            Assert.assertEquals(sVar.getDy(), f[idx++], 1e-12);
            Assert.assertEquals(sVar.getDth(), f[idx++], 1e-12);
        }
    }

    @Test
    public void getCenterX() {
        float[] f = new float[]{0.05f, 0.21f, 0.40f, 0.33f, 0.23f, 0.94f, 0.68f, 0.96f, 0.44f, 0.94f, 0.01f, 0.61f,
                0.80f, 0.23f, 0.93f, 0.76f, 0.83f, 0.57f, 0.79f, 0.33f, 0.22f, 0.31f, 0.58f, 0.83f, 0.29f, 0.40f, 0.86f, 0.61f, 0.99f, 0.20f, 0.83f, 0.68f, 0.25f, 0.48f, 0.40f, 0.60f, 0.80f, 0.11f, 0.82f, 0.84f, 0.35f, 0.43f, 0.57f, 0.70f, 0.74f, 0.76f, 0.39f, 0.43f, 0.96f, 0.57f, 0.85f, 0.28f, 0.62f, 0.59f, 0.96f, 0.09f, 0.50f, 0.52f, 0.09f, 0.90f, 0.88f, 0.44f, 0.78f, 0.15f, 0.62f, 0.26f, 0.45f, 0.84f, 0.20f, 0.30f, 0.48f, 0.34f};
        State s = new State(f, false);
        Assert.assertEquals(f[0], s.getCenterX(), 1e-12);

        f[0] = 100;
        Assert.assertEquals(f[0], new State(f, false).getCenterX(), 1e-12);
    }

    @Test
    public void getStateVariableLength() {
        float[] f = new float[]{0.05f, 0.21f, 0.40f, 0.33f, 0.23f, 0.94f, 0.68f, 0.96f, 0.44f, 0.94f, 0.01f, 0.61f,
                0.80f, 0.23f, 0.93f, 0.76f, 0.83f, 0.57f, 0.79f, 0.33f, 0.22f, 0.31f, 0.58f, 0.83f, 0.29f, 0.40f, 0.86f, 0.61f, 0.99f, 0.20f, 0.83f, 0.68f, 0.25f, 0.48f, 0.40f, 0.60f, 0.80f, 0.11f, 0.82f, 0.84f, 0.35f, 0.43f, 0.57f, 0.70f, 0.74f, 0.76f, 0.39f, 0.43f, 0.96f, 0.57f, 0.85f, 0.28f, 0.62f, 0.59f, 0.96f, 0.09f, 0.50f, 0.52f, 0.09f, 0.90f, 0.88f, 0.44f, 0.78f, 0.15f, 0.62f, 0.26f, 0.45f, 0.84f, 0.20f, 0.30f, 0.48f, 0.34f};
        State s = new State(f, false);

        Assert.assertEquals(12, s.getStateVariableCount());
        Assert.assertEquals(12, s.getAllStateVariables().length);
    }
}