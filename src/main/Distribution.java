package main;

import java.util.ArrayList;
import java.util.Random;

public abstract class Distribution<T> {

    protected static Random rand = new Random();

    /**
     * Return a sample from the distribution.
     **/
    public abstract T randOnDistribution(ArrayList<T> set);

    /**
     * Return a random sample from the set.
     **/
    public T randSample(ArrayList<T> set) {
        return set.get(randInt(0, set.size() - 1));
    }

    /**
     * Use this distribution's rules to choose between two sets.
     * Makes the distribution also work for the tree policy
     * Returns true for set 1, false for set 2.
     **/
    public boolean chooseASet(ArrayList<T> set1, ArrayList<T> set2) {
        ArrayList<T> totalSet = new ArrayList<T>();
        totalSet.addAll(set1);
        totalSet.addAll(set2);

        T sample = randOnDistribution(totalSet);

        return set1.contains(sample);
    }

    /**
     * Generate a random integer between two values, inclusive.
     **/
    public static int randInt(int min, int max) {
        if (min > max)
            throw new IllegalArgumentException("Random int sampler should be given a minimum value which is less than or equal to the given max value.");
        int randomNum = rand.nextInt((max - min) + 1) + min;
        return randomNum;
    }
}
