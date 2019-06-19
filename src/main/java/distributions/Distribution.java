package distributions;

import game.action.Action;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static tree.Utility.randInt;

/**
 * Framework for sampling from a discrete set on different distributions. Extend this to make specific distribution
 * types.
 *
 * @param <T> The types of objects to be sampled over, typically {@link Action game.action}.
 */
public abstract class Distribution<T> {

    static Random rand = new Random();

    /**
     * Return a sample from a given set according to the rules of the distribution. Selection should be random, but
     * weighted in whatever way the distribution defines.
     *
     * @param pool Set of objects from which the sample will be selected.
     * @return An element of the input list selected randomly according to the rules of the distribution.
     */
    public abstract T randOnDistribution(List<T> pool);


    /**
     * Return a random sample from a given set. The element is randomly selected without any regards to the
     * distribution's rules.
     *
     * @param pool Set of objects from which the sample will be selected.
     * @return An element of the input list selected randomly according to the rules of the distribution.
     */
    public T randSample(List<T> pool) {
        if (pool.size() < 1)
            throw new IllegalArgumentException("Argument sampling pool must contain at least 1 element.");

        return pool.get(randInt(0, pool.size() - 1));
    }

    /**
     * Choose between two lists of objects according to the rules of the distribution. A sample is picked from the
     * combined list of objects and then checked to see which input list it came from.
     *
     * @param list1 First candidate list of objects.
     * @param list2 Second candidate list of objects.
     * @return Boolean representing which candidate list was chosen. True means list1, false means list2.
     */
    public boolean chooseASet(List<T> list1, List<T> list2) {
        if (list1.size() < 1 && list2.size() < 1)
            throw new IllegalArgumentException("At least one of the lists must have at least one element.");

        List<T> totalSet = new ArrayList<>();
        totalSet.addAll(list1);
        totalSet.addAll(list2);

        T sample = randOnDistribution(totalSet);

        return list1.contains(sample);
    }
}
