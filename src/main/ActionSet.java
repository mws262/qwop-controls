package main;

import distributions.Distribution;

import java.util.ArrayList;

/**
 * An ActionSet acts like an ArrayList, except it allows for sampling on a Distribution.
 *
 * @author Matt
 */
public class ActionSet extends ArrayList<Action> {

    private static final long serialVersionUID = 1L;

    public Distribution<Action> samplingDist;

    private ActionSet(Distribution<Action> samplingDist) {
        this.samplingDist = samplingDist;
    }

    /**
     * Get a non-skewed random element.
     **/
    public Action getRandom() {
        return samplingDist.randSample(this);
    }

    /**
     * Get a random sample from the defined distribution.
     **/
    public Action sampleDistribution() {
        return samplingDist.randOnDistribution(this);
    }

    /**
     * Duplicate this, including the elements.
     **/
    @Override
    public ActionSet clone() {
        ActionSet duplicate = new ActionSet(samplingDist);
        duplicate.addAll(this);
        return duplicate;
    }

    /**
     * Get an ActionSet defined by as many durations as desired, 4 keys, and a sampling distribution.
     **/
    public static ActionSet makeActionSet(Integer[] durations, boolean[][] keys, Distribution<Action> dist) {
        ActionSet set = new ActionSet(dist);
        for (int i = 0; i < durations.length; i++) {
            set.add(new Action(durations[i], keys[i]));
        }
        return set;
    }

    /**
     * Simply return many instances of the given keyString. Useful when making action sets sometimes.
     **/
    public static boolean[][] replicateKeyString(boolean[] keyString, int times) {
        boolean[][] bigger = new boolean[times][];

        for (int i = 0; i < times; i++) {
            bigger[i] = keyString;
        }

        return bigger;
    }


}
