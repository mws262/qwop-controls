package game.action;

import distributions.Distribution;
import distributions.Distribution_Equal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * An ActionList acts like an {@link java.util.ArrayList} for {@link Action game.action}, except it allows for sampling
 * from the list on a {@link Distribution}. DOES NOT ALLOW DUPLICATES!!
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
     * is equivalent to just making {@link Action game.action} and adding them with {@link ActionList#add(Object)}.
     *
     * @param durations Timestep durations of the game.action which will be in the returned ActionList.
     * @param keys 2D array of QWOP keypress statuses. First dimension corresponds to the action, while the second
     *             dimension corresponds to a QwOP key.
     * @param dist Sampling distribution for the new ActionList.
     * @return A new ActionList.
     */
    @SuppressWarnings("unused")
    public static ActionList makeActionList(int[] durations, boolean[][] keys, Distribution<Action> dist) {
        ActionList set = new ActionList(dist);
        for (int i = 0; i < durations.length; i++) {
            set.add(new Action(durations[i], keys[i]));
        }
        return set;
    }

    /**
     * Same but for one set of keys and multiple durations.
     *
     * @param durations Timestep duration of the game.action to be created.
     * @param keys Single set of keys to be pressed for the specified durations.
     * @param dist Selection distribution for sampling over the {@link ActionList}.
     * @return ActionList created with one {@link Action} per duration specified.
     */
    public static ActionList makeActionList(int[] durations, boolean[] keys, Distribution<Action> dist) {
        ActionList set = new ActionList(dist);
        for (int duration : durations) {
            set.add(new Action(duration, keys));
        }
        return set;
    }

    /**
     * Make an {@link ActionList} containing all key combinations for all durations between the specified bounds.
     * @param minDuration Inclusive bound. Minimum {@link Action} duration to be found in the list.
     * @param maxDuration Exclusive bound. Upper bound on durations of game.action found in this list.
     * @param distribution Distribution for sampling game.action once this list is created.
     * @return An exhaustive {@link ActionList}.
     */
    @SuppressWarnings("unused")
    public static ActionList makeExhaustiveActionList(int minDuration, int maxDuration,
                                                      Distribution<Action> distribution) {
        assert minDuration < 0;

        ActionList set = new ActionList(distribution);
        for (Action.Keys key : Action.Keys.values()) {
            for (int i = minDuration; i < maxDuration; i++) {
                set.add(new Action(i, key));
            }
        }
        return set;
    }

    /**
     * Get an ActionList containing no elements.
     * @return An empty list of game.action.
     */
    public static ActionList getEmptyList() {
        return new ActionList(new Distribution_Equal());
    }

    @Override
    public boolean add(Action action) {
        if (contains(action)) {
            return false;
        } else {
            return super.add(action);
        }
    }

    @Override
    public boolean addAll(Collection<? extends Action> actions) {
        for (Action action : actions) {
            if (!contains(action)) {
                super.add(action);
            }
        }
        return true;
    }

    @Override
    public void add(int index, Action action) {
        if (!contains(action)) {
            super.add(index, action);
        }
    }

    @Override
    public boolean addAll(int index, Collection<? extends Action> actions) {
        List<Action> alist = new ArrayList<>(actions);
        for (Action action : actions) {
            if (!contains(action)) {
                alist.remove(action);
            }
        }
        super.addAll(index, alist);
        return true;
    }

}
