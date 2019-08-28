package game.cartpole;

import com.google.common.base.Preconditions;
import game.IGameInternal;
import game.state.IState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deeplearning4j.gym.Client;
import org.deeplearning4j.gym.StepReply;
import org.deeplearning4j.rl4j.space.Box;
import org.deeplearning4j.rl4j.space.DiscreteSpace;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

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
public class CartPole implements IGameInternal<CommandCartPole> { // TODO implement one of the game interfaces.

    /**
     * StateQWOP space dimension. [cart position, cart velocity, pole angle, pole velocity at tip]
     */
    public static final int STATE_SIZE = 4;

    /**
     * Number of command choices available at every timestep. One hot encoding, [1, 0] is push left, [0, 1] is push
     * right.
     */
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
     * State when balanced perfectly upright, centered, and with no velocity.
     */
    private static final StateCartPole balancedState = new StateCartPole(0, 0, 0, 0, false);

    /**
     * StateQWOP vector at the after the most recent command (or a resetGame).
     */
    private StateCartPole currentState = balancedState;

    /**
     * Reward seen from the most recent step.
     */
    private float lastReward;

    /**
     * Total reward seen this episode.
     */
    private double cumulativeReward;

    private int timestepsThisGame;

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
            resetGame();
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
    public void resetGame() {
        logger.debug("Game reset called.");

        Preconditions.checkArgument(client != null, "Must connect to the python http server before using this. Run " +
                "gym_http_server.py.");
        double[] st = client.reset().toArray();

        currentState = new StateCartPole((float) st[0], (float) st[1], (float) st[2], (float) st[3], false);
        cumulativeReward = 0;
        timestepsThisGame = 0;
    }

    /**
     * Step the game forward one physics timestep.
     * @return Whether the step was taken successfully. Doesn't mean that the the thing didn't fall down! It just
     * means that the code executed correctly without messing up messages to the HTTP server.
     */
    public void step(@NotNull CommandCartPole command) {
        if (isClosing) // If the game is being shut down, don't attempt to step again or it will probably pull the
            // simulation window back up when the step command is sent.
            return;
        try {
            StepReply<Box> observation = client.step(command.equals(CommandCartPole.LEFT) ? 0 : 1); // only 0 or 1 --
            // push left or right.
            double[] st = observation.getObservation().toArray();
            currentState = new StateCartPole((float) st[0], (float) st[1], (float) st[2], (float) st[3],
                    observation.isDone());
            lastReward = (float) observation.getReward(); // 1 for survival.
            cumulativeReward += lastReward;
            timestepsThisGame++;
        } catch (RuntimeException e) {
            // Lost connection. Most likely closed on the gym server side.
            logger.warn("Server connection lost during a step command.");
        }
    }

    @Override
    public void command(@NotNull CommandCartPole command) {
        step(command);
    }

    @Override
    public int getNumberOfChoices() {
        return ACTIONSPACE_SIZE;
    }

    @Override
    public void draw(Graphics g, float runnerScaling, int xOffsetPixels, int yOffsetPixels) {
        // TODO
    }

    @Override
    public void setState(IState st) {} // TODO

    @Override
    public IGameInternal<CommandCartPole> getCopy() {
        return null;
    }

    /**
     * Get the system state as of the most recent call to {@link CartPole#step(CommandCartPole)}.
     * @return 4-element array of state variables.
     */
    public IState getCurrentState() {
        return currentState;
    }

    /**
     * Does the Gym environment consider the game over: leaves the screen, pole falls beyond 15 degrees, timesteps
     * exceed 200. Note that you can continue to step the game after this point, but the environment scores it as over.
     * @return Whether this game episode is over.
     */
    @Override
    public boolean isFailed() {
        return currentState.isFailed();
    }

    @Override
    public long getTimestepsThisGame() {
        return timestepsThisGame;
    }

    @Override
    public int getStateDimension() {
        return STATE_SIZE;
    }

    /**
     * Get the reward accrued from the most recent call to {@link CartPole#step(CommandCartPole)}. It's trivial for this
     * environment: just gets 1 for every successful, unfallen timestep.
     * @return Reward from the most recent timestep.
     */
    public float getLastReward() {
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
            cartPole.step(CommandCartPole.LEFT);
            if (cartPole.isFailed())
                cartPole.resetGame();
        }
    }

}
