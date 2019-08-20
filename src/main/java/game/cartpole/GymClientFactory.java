package game.cartpole;

import org.deeplearning4j.gym.Client;
import org.deeplearning4j.gym.ClientFactory;
import org.deeplearning4j.gym.ClientUtils;
import org.deeplearning4j.rl4j.space.ActionSpace;
import org.deeplearning4j.rl4j.space.GymObservationSpace;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Same result as {@link ClientFactory} in dl4j except that it allows the random seed inside the Gym environment to
 * be set.
 */
public class GymClientFactory extends ClientFactory {

    // Same as the version in DL4J except this allows the random seed to be set.

    /**
     * Create a new HTTP client to connect to the gym-http-api. Make sure to run gym_http_server.py first!
     * @param envId Name of the environment, like "CartPole-v0".
     * @param seed Random seed used by the environment, likely to generate initial conditions.
     * @param render Display the visualizer?
     * @return A new client connected to the gym-http-api server.
     */
    public static <O, A, AS extends ActionSpace<A>> Client<O, A, AS> build(String envId, long seed, boolean render) {
        String url = "http://127.0.0.1:5000";
        JSONObject body = (new JSONObject()).put("env_id", envId).put("seed", seed);
        JSONObject reply = ClientUtils.post(url + Client.ENVS_ROOT, body).getObject();

        String instanceId;
        try {
            instanceId = reply.getString("instance_id");
        } catch (JSONException var8) {
            throw new RuntimeException("Environment id not found", var8);
        }

        GymObservationSpace<O> observationSpace = fetchObservationSpace(url, instanceId);
        AS actionSpace = fetchActionSpace(url, instanceId);
        return new Client<>(url, envId, instanceId, observationSpace, actionSpace, render);
    }
}
