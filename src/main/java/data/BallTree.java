//package data;
//
//import com.sun.tools.javac.util.Pair;
//import game.State;
//import org.jblas.util.Random;
//
//import java.io.Serializable;
//import java.util.*;
//import java.util.stream.Collectors;
//
//public class BallTree implements Serializable {
//
//    /**
//     * Stats associated with each body coordinate so rescaling can be done.
//     */
//    private Map<Pair<State.ObjectName, State.StateName>, StateStats> stateStats = new HashMap<>();
//
//    /**
//     * Total number of states indexed by this tree. Currently, the tree must be rebuilt to add additional states.
//     */
//    final int totalStateCount;
//
//    /**
//     * Number of body coordinates (e.g. torso-x, ruarm-dy...).
//     */
//    private final static int numValues = State.StateName.values().length * State.ObjectName.values().length;
//
//    /**
//     * Create a new ball tree for doing nearest neighbor calculations on boatloads of states.
//     *
//     * @param allStatesToAdd A list of all states to include TODO should include linking along a single sequence...
//     */
//    public BallTree(List<State> allStatesToAdd) {
//
//        totalStateCount = allStatesToAdd.size();
//
//        // Go through all the body coordinates and calculate some simple statistics about it.
//        for (Pair<State.ObjectName, State.StateName> stateLabel : prioritizedStates) {
//            StateStats stateStat = new StateStats();
//            stateStats.put(stateLabel, stateStat);
//
//            float min = Float.MAX_VALUE;
//            float max = -Float.MAX_VALUE;
//
//            // Calculate the mean for this one body-coordinate.
//            for (State state : allStatesToAdd) {
//                float sval = state.getStateVarFromName(stateLabel.fst, stateLabel.snd);
//                stateStat.mean += sval;
//                if (sval > max) {
//                    max = sval;
//                }
//                if (sval < min) {
//                    min = sval;
//                }
//            }
//            stateStat.mean /= (float)totalStateCount;
//
//            // Calculate the stdev for this one body-coordinate.
//            for (State state : allStatesToAdd) {
//                float sval = state.getStateVarFromName(stateLabel.fst, stateLabel.snd);
//                float svalMinusMean = sval - stateStat.mean;
//                stateStat.standardDeviation += svalMinusMean * svalMinusMean;
//            }
//
//            stateStat.standardDeviation /= (float)(totalStateCount - 1);
//            stateStat.standardDeviation = (float) Math.sqrt(stateStat.standardDeviation);
//            stateStat.normalizedRange = (max - min) / stateStat.standardDeviation;
//        }
//
//    }
//
//
//    class BallCluster {
//
//        // Centroid of the cluster (basically an imaginary state).
//        BallClusterCentroid centroid;
//
//        // Right child cluster.
//        BallCluster rightChild;
//
//        // Left child cluster.
//        BallCluster leftChild;
//
//    }
//
//    class BallClusterCentroid extends State {
//        public BallClusterCentroid() {
//            super(bodyS, headS, rthighS, lthighS, rcalfS, lcalfS, rfootS, lfootS, ruarmS, luarmS, rlarmS, llarmS, isFailed);
//        }
//    }
//
//    class StateStats {
//        float mean;
//        float standardDeviation;
//        float normalizedRange;
//    }
//
//    static class SamplePoint {
//        float[] data;
//        SamplePoint() {
//            data = new float[]{Random.nextFloat(), Random.nextFloat(), Random.nextFloat(), Random.nextFloat(),
//                    Random.nextFloat(), Random.nextFloat()};
//        }
//    }
//
//    public static void main(String[] args) {
//        BallTree ballTree = new BallTree();
//
//        List<SamplePoint> samplePointList = new ArrayList<>();
//        for (int i = 0; i < 100; i++) {
//            samplePointList.add(new SamplePoint());
//        }
//
//    }
//}
