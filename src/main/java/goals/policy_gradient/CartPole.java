package goals.policy_gradient;

import com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deeplearning4j.gym.Client;
import org.deeplearning4j.gym.ClientUtils;
import org.deeplearning4j.gym.StepReply;
import org.deeplearning4j.rl4j.space.Box;
import org.deeplearning4j.rl4j.space.DiscreteSpace;
import org.json.JSONObject;

// Info from the OpenAi side: https://github.com/openai/gym-http-api
// Info from the Java side: https://github.com/eclipse/deeplearning4j/tree/master/gym-java-client
// Notes: Need to run the server (python gym_http_server.py) first. Should probably use python2. I didn't have any
// luck with 3.x.
public class CartPole { // TODO implement one of the game interfaces.

    public static final int STATE_SIZE = 4;

    /**
     * Number of action choices available at every timestep.
     */
    public static final int ACTIONSPACE_SIZE = 2;

    /**
     * Handles connection to the game server. All incoming and outgoing information must pass through this.
     */
    private Client<Box, Integer, DiscreteSpace> client;

    /**
     * State vector at the after the most recent action (or a reset).
     */
    private double[] currentState;

    private double lastReward;

    private double cumulativeReward;

    private boolean isDone = false;

    Logger logger = LogManager.getLogger(CartPole.class);

    public void connect(boolean withGraphics) {
        try {
            client = GymClientFactory.build("CartPole-v0", 1, withGraphics);
            JSONObject body = (new JSONObject()).put("seed", 1);
            JSONObject reply = ClientUtils.post("http://127.0.0.1:5000" + Client.ENVS_ROOT, body).getObject();
            reset();
            logger.info("Connected successfully to the Python server.");
        } catch(RuntimeException e) {
            logger.warn("Could not connect to the cartpole Python server. Run gym_http_server.py. Attempting to " +
                    "reconnect in a few seconds.");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            connect(withGraphics);
        }
    }

    public void reset() {

        Preconditions.checkArgument(client != null, "Must connect to the python http server before using this. Run " +
                "gym_http_server.py.");
        currentState = client.reset().toArray();
        isDone = false;
        cumulativeReward = 0;
    }

    public void step(int action) { // TODO genericize.
        StepReply<Box> observation = client.step(action); // only 0 or 1 -- push left or right.
        currentState = observation.getObservation().toArray();
        isDone = observation.isDone();
        lastReward = observation.getReward(); // 1 for survival.
        cumulativeReward += lastReward;
    }

    public double[] getCurrentState() {
        return currentState;
    }

    public boolean isDone() {
        return isDone;
    }

    public double getLastReward() {
        return lastReward;
    }

    public double getCumulativeReward() {
        return cumulativeReward;
    }

    public static float[] toFloatArray(double[] in) {
        float[] out = new float[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = (float) in[i];
        }
        return out;
    }

    // Just to test it out.
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
