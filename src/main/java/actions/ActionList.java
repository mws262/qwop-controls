package actions;

import distributions.Distribution;
import distributions.Distribution_Equal;

import java.util.ArrayList;
import java.util.List;

/**
 * An ActionList acts like an {@link java.util.ArrayList} for {@link Action actions}, except it allows for sampling
 * from the list on a {@link Distribution}.
 *
 * @author Matt
 * @see Action
 * @see ActionQueue
 * @see IActionGenerator
 * @see Distribution
 */
public class ActionList extends ArrayList<Action> {

    public Distribution<Action> samplingDist;

    /**
     * Create a new ActionList which can sample according to the rules of a {@link Distribution}. It may otherwise be
     * treated as an {@link ArrayList}.
     *
     * @param samplingDist Distribution that samples of the action set will be pulled when calling
     * {@link ActionList#sampleDistribution}.
     */
    public ActionList(Distribution<Action> samplingDist) {
        this.samplingDist = samplingDist;
    }

    /**
     * Get an element from this ActionList at random.
     *
     * @return A random element of this ActionList.
     * @see Distribution#randSample(List)
     */
    public Action getRandom() {
        return samplingDist.randSample(this);
    }

    /**
     * Get a random sample from the defined distribution.
     *
     * @return An action sampled from this ActionList according to its defined {@link Distribution}.
     **/
    public Action sampleDistribution() {
        return samplingDist.randOnDistribution(this);
    }

    /**
     * Duplicate this ActionList, producing a copy with the same {@link Action} elements and same sampling
     * {@link Distribution}.
     *
     * @return A getCopy of this ActionList.
     */
    public ActionList getCopy() {
        ActionList duplicate = new ActionList(samplingDist);
        duplicate.addAll(this);
        return duplicate;
    }

    /**
     * Get an ActionList defined by as many durations as desired, 4 keys for each, and a sampling distribution. This
     * is equivalent to just making {@link Action actions} and adding them with {@link ActionList#add(Object)}.
     *
     * @param durations Timestep durations of the actions which will be in the returned ActionList.
     * @param keys 2D array of QWOP keypress statuses. First dimension corresponds to the action, while the second
     *             dimension corresponds to a QwOP key.
     * @param dist Sampling distribution for the new ActionList.
     * @return A new ActionList.
     */
    public static ActionList makeActionSet(int[] durations, boolean[][] keys, Distribution<Action> dist) {
        ActionList set = new ActionList(dist);
        for (int i = 0; i < durations.length; i++) {
            set.add(new Action(durations[i], keys[i]));
        }
        return set;
    }

    /**
     * Same but for one set of keys and multiple durations.
     *
     * @param durations
     * @param keys
     * @param dist
     * @return
     */
    public static ActionList makeActionSet(int[] durations, boolean[] keys, Distribution<Action> dist) {
        ActionList set = new ActionList(dist);
        for (int duration : durations) {
            set.add(new Action(duration, keys));
        }
        return set;
    }

    public static ActionList makeExhaustiveActionSet(int minDuration, int maxDuration) {
        assert minDuration < 0;

        ActionList set = new ActionList(new Distribution_Equal());
        for (Action.Keys key : Action.Keys.values()) {
            for (int i = minDuration; i < maxDuration; i++) {
                set.add(new Action(i, key));
            }
        }
        return set;
    }

    /**
     * Simply return many instances of the given keyString. Useful when making action sets sometimes.
     *
     * @param keyString Boolean array representing QWOP keys.
     * @param times Number of times to replicate the given keyString in the 1st dimension.
     * @return
     */
    @Deprecated
    public static boolean[][] replicateKeyString(boolean[] keyString, int times) {
        boolean[][] bigger = new boolean[times][];

        for (int i = 0; i < times; i++) {
            bigger[i] = keyString;
        }
        return bigger;
    }

    /**
     * Get an ActionList containing no elements.
     * @return An empty list of actions.
     */
    public static ActionList getEmptyList() {
        return new ActionList(new Distribution_Equal());
    }
}
