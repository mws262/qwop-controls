package distributions;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static main.Utility.randInt;

/**
 * 
 *
 * @param <T>
 */
public abstract class Distribution<T> {

    protected static Random rand = new Random();

    /**
     * Return a sample from the distribution.
     **/
    public abstract T randOnDistribution(List<T> set);

    /**
     * Return a random sample from the set.
     **/
    public T randSample(List<T> set) {
        return set.get(randInt(0, set.size() - 1));
    }

    /**
     * Use this distribution's rules to choose between two sets.
     * Makes the distribution also work for the tree policy
     * Returns true for set 1, false for set 2.
     **/
    public boolean chooseASet(List<T> set1, List<T> set2) {
        List<T> totalSet = new ArrayList<>();
        totalSet.addAll(set1);
        totalSet.addAll(set2);

        T sample = randOnDistribution(totalSet);

        return set1.contains(sample);
    }
}
