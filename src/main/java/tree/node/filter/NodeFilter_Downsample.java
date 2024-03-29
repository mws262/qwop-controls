package tree.node.filter;

import game.action.Command;
import game.state.IState;
import tree.Utility;
import tree.node.NodeGameExplorableBase;

import java.util.Iterator;
import java.util.List;

/**
 * Filter which reduces the number of nodes in a list. Usually done for visualization or computational reasons. Only
 * applies to lists of nodes. Single node evaluations will always return true.
 *
 * @author matt
 */
public class NodeFilter_Downsample<C extends Command<?>, S extends IState> implements INodeFilter<C, S> {

    /**
     * Given a list, the downsampler will keep a maximum of this number of nodes.
     */
    private final int maxNodesToKeep;

    /**
     * What strategy is used to downsample?
     */
    private final Strategy downsamplingStrategy;

    /**
     * Available strategies for downsampling.
     */
    public enum Strategy {
        EVENLY_SPACED, RANDOM
    }

    /**
     * Create a new downsampling tree.node.filter. This constructor will default to an evenly-spaced downsampling of nodes.
     *
     * @param maxNodesToKeep Maximum allowable nodes after filtering a list of nodes.
     */
    public NodeFilter_Downsample(int maxNodesToKeep) {
        if (maxNodesToKeep < 0)
            throw new IllegalArgumentException("Cannot have a negative maximum number of nodes to keep.");

        this.maxNodesToKeep = maxNodesToKeep;

        downsamplingStrategy = Strategy.EVENLY_SPACED;
    }

    /**
     * Create a new downsampling tree.node.filter. This constructor will default to an evenly-spaced downsampling of nodes.
     *
     * @param maxNodesToKeep Maximum allowable nodes after filtering a list of nodes.
     * @param filteringStrategy Which strategy should be used for downsampling ({@link Strategy strategies}).
     */
    @SuppressWarnings("unused")
    public NodeFilter_Downsample(int maxNodesToKeep, Strategy filteringStrategy) {
        if (maxNodesToKeep < 0)
            throw new IllegalArgumentException("Cannot have a negative maximum number of nodes to keep.");

        this.maxNodesToKeep = maxNodesToKeep;

        downsamplingStrategy = filteringStrategy;
    }

    @Override
    public void filter(List<? extends NodeGameExplorableBase<?, C, S>> nodes) {
        if (maxNodesToKeep == 0) {
            nodes.clear();
            return;
        }
        int numNodes = nodes.size();
        if (numNodes > maxNodesToKeep) { // If we already have <= the max number of nodes, no need to downsample.
            switch (downsamplingStrategy) {
                case EVENLY_SPACED:
                    float ratio = numNodes / (float) maxNodesToKeep;

                    if (ratio > 1) {
                        Iterator<? extends NodeGameExplorableBase<?, C, S>> iter = nodes.iterator();
                        int count = 0;
                        float keepCount = Float.MIN_VALUE;
                        while (iter.hasNext()) {
                            iter.next();
                            if (Math.ceil(keepCount) == count) {
                                keepCount += ratio;
                            } else {
                                iter.remove();
                            }
                            count++;
                        }
                    }
                    break;
                case RANDOM:
                    while (nodes.size() > maxNodesToKeep) {
                        int idxToRemove = Utility.randInt(0, nodes.size() - 1);
                        nodes.remove(idxToRemove);
                    }
                    break;
                default:
                    throw new IllegalStateException("Unknown downsampling strategy.");
            }
            assert nodes.size() <= maxNodesToKeep : nodes.size() + " exceeds limit of " + maxNodesToKeep;
        }
    }
}
