package game.cartpole;

import com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deeplearning4j.gym.Client;
import org.deeplearning4j.gym.StepReply;
import org.deeplearning4j.rl4j.space.Box;
import org.deeplearning4j.rl4j.space.DiscreteSpace;

/**
 * Interface to DL4J's HTTP interface to OpenAI's HTTP interface to their Gym cart-pole environment. Used for debugging
 * and testing various algorithms on a simpler system. All this does beyond the DL4J version is set the random seed
 * for the initial conditions, provide some error checking, and other conveniences.
 *
 * The Python OpenAI HTTP Gym server must be running for it to do anything. Otherwise it will issue a logged warning,
 * wait a little bit and attempt to reconnect (while blocking the thread that calls {@link CartPole#connect(boolean)}
 * ). To start the python client.
 * <ol>
 *     <li>Clone https://github.com/openai/gym-http-api.</li>
 *     <li>Install all the items in <code>requirements.txt</code>. Use pip2 (have had no success with Python 3.7).</li>
 *     <li><code>python [or python2 or py -2] gym_http_server.py</code> If a JSON error about float32's, it's
 *     probably the Python version. Other errors are probably library dependencies.</li>
 * </ol>
 *
 * @see: <a href="https://github.com/eclipse/deeplearning4j/tree/master/gym-java-client">DL4J HTTP client Github</a>.
 *
 * @author matt
 */
public class CartPole { // TODO implement one of the game interfaces.

    /**
     * State space dimension. [cart position, cart velocity, pole angle, pole velocity at tip]
     */
    @SuppressWarnings("WeakerAccess")
    public static final int STATE_SIZE = 4;

    /**
     * Number of action choices available at every timestep. One hot encoding, [1, 0] is push left, [0, 1] is push
     * right.
     */
    @SuppressWarnings("WeakerAccess")
    public static final int ACTIONSPACE_SIZE = 2;

    /**
     * Handles connection to the game server. All incoming and outgoing information must pass through this.
     */
    private Client<Box, Integer, DiscreteSpace> client;

    /**
     * Random seed used by the cartpole environment to pick initial conditions. It's nice to have this set to have
     * deterministic training if needed.
     */
    private final int randomSeed = 1;

    /**
     * State vector at the after the most recent action (or a reset).
     */
    private float[] currentState = new float[STATE_SIZE];

    /**
     * Reward seen from the most recent step.
     */
    private double lastReward;

    /**
     * Total reward seen this episode.
     */
    private double cumulativeReward;

    /**
     * Does Gym say the game is over? Either +/- 15 deg on the pole, leaves the screen, or reaches 200 timesteps.
     */
    private boolean isDone = false;

    /**
     * Logging. Add -Dlog4j.configurationFile="./src/main/resources/log4j2.xml" to VM options if logging isn't working.
     */
    private static final Logger logger = LogManager.getLogger(CartPole.class);

    /**
     * Is the environment being closed down? Keeps step from being called after close has happened (which just brings
     * the viewer window back for some reason).
     */
    private volatile boolean isClosing = false;

    /**
     * Connect to the Gym server that runs the CartPole game.
     *
     * @param withGraphics Display the graphics window or do as headless. It's a little faster without graphics.
     */
    public void connect(boolean withGraphics) {
        try {
            client = GymClientFactory.build("CartPole-v0", randomSeed, withGraphics);
            reset();
            logger.info("Connected successfully to the Python server.");
        } catch(RuntimeException e) {
            e.printStackTrace();
            logger.warn("Could not connect to the cartpole Python server. Run gym_http_server.py. Attempting to " +
                    "reconnect in a few seconds.");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            connect(withGraphics);
        }

        // Try to kill the connection if prematurely ended.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (client != null) {
                isClosing = true;
                client.monitorClose();
            }
        }));
    }

    /**
     * Reset the environment. Will also set the current state to be the new initial condition. Note that it picks
     * initial conditions randomly based on the seed sent to Gym.
     */
    public void reset() {
        Preconditions.checkArgument(client != null, "Must connect to the python http server before using this. Run " +
                "gym_http_server.py.");
        double[] st = client.reset().toArray();
        for (int i = 0; i < st.length; i++) {
            currentState[i] = (float) st[i];
        }
        isDone = false;
        cumulativeReward = 0;
    }

    /**
     * Step the game forward one physics timestep.
     * @param action Index of the action to take. 0 is push left, 1 is push right.
     * @return Whether the step was taken successfully. Doesn't mean that the the thing didn't fall down! It just
     * means that the code executed correctly without messing up messages to the HTTP server.
     */
    public boolean step(int action) { // TODO genericize.
        if (isClosing)
            return false;
        try {
            StepReply<Box> observation = client.step(action); // only 0 or 1 -- push left or right.
            double[] st = observation.getObservation().toArray();
            for (int i = 0; i < st.length; i++) {
                currentState[i] = (float) st[i];
            }
            isDone = observation.isDone();
            lastReward = observation.getReward(); // 1 for survival.
            cumulativeReward += lastReward;
            return true;
        } catch (RuntimeException e) {
            // Lost connection. Most likely closed on the gym server side.
            return false;
        }
    }

    /**
     * Get the system state as of the most recent call to {@link CartPole#step(int)}.
     * @return 4-element array of state variables.
     */
    public float[] getCurrentState() {
        return currentState;
    }

    /**
     * Does the Gym environment consider the game over: leaves the screen, pole falls beyond 15 degrees, timesteps
     * exceed 200. Note that you can continue to step the game after this point, but the environment scores it as over.
     * @return Whether this game episode is over.
     */
    public boolean isDone() {
        return isDone;
    }

    /**
     * Get the reward accrued from the most recent call to {@link CartPole#step(int)}. It's trivial for this
     * environment: just gets 1 for every successful, unfallen timestep.
     * @return Reward from the most recent timestep.
     */
    public double getLastReward() {
        return lastReward;
    }

    /**
     * Total reward accrued for all timesteps this game. Maxes out at 200, which is considered a fully successful
     * episode.
     * @return Total reward accumulated over the course of the current episode.
     */
    public double getCumulativeReward() {
        return cumulativeReward;
    }

    /**
     * Run with fixed actions just to test out.
     */
    public static void main(String[] args) {
        CartPole cartPole = new CartPole();
        cartPole.connect(true);

        for (int i = 0; i < 100; i++) {
            cartPole.step(0);
            if (cartPole.isDone)
                cartPole.reset();
        }
    }
}
