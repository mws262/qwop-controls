package transformations;

import java.util.List;
import java.util.stream.Collectors;

import org.jblas.FloatMatrix;
import org.jblas.Singular;

import game.State;

public class Transform_PCA implements ITransform {

    /**
     * Eigenvectors found during SVD of the conditioned states. They represent the principle directions that explain
     * most of the state variance.
     **/
    private FloatMatrix evecs;

    /**
     * Number of state values per node as seen in the calculations.
     **/
    private final int numStates = State.StateName.values().length * State.ObjectName.values().length;

    /**
     * Averages of the states of the data given to calculate the PCA.
     **/
    private FloatMatrix stateAvgs = new FloatMatrix(1, numStates);

    /**
     * Standard deviations of the states of the data given to calculate the PCA.
     **/
    private FloatMatrix stateSTDs = new FloatMatrix(1, numStates);

    /**
     * PCA components used to do transforms. Usually will only be the first couple.
     **/
    private int[] transformPCAComponents;

    /**
     * Make a new PCA transformer. Note that it will do the full PCA, but will only used
     * the specified components for future transforms. In this case, that means that the
     * reduced state will have transformPCAComponents.length components.
     *
     * @param transformPCAComponents
     */
    public Transform_PCA(int[] transformPCAComponents) {
        this.transformPCAComponents = transformPCAComponents;
    }

    @Override
    public void updateTransform(List<State> nodesToUpdateFrom) {
        // Do PCA!
        FloatMatrix dataSet = conditionData(unpackData(nodesToUpdateFrom));
        FloatMatrix[] USV = Singular.fullSVD(dataSet);
        evecs = USV[2]; // Eigenvectors
        /** During SVD we find the eigenvalues, the weights for what portion of variance is explained by the
         * corresponding eigenvector. **/
        FloatMatrix evals = USV[1].mul(USV[1]).div(dataSet.rows);

        // Also make the vector of normalized eigenvalues.
        float evalSum = 0;
        for (int i = 0; i < evals.length; i++) {
            evalSum += evals.get(i);
        }
        /** Normalized so the sum of the evals == 1 **/
        FloatMatrix evalsNormalized = new FloatMatrix(evals.length);
        for (int i = 0; i < evals.length; i++) {
            evalsNormalized.put(i, evals.get(i) / evalSum);
        }

    }

    @Override
    public List<float[]> transform(List<State> originalStates) {
        FloatMatrix preppedDat = unpackData(originalStates);
        preppedDat.subiRowVector(stateAvgs);
        preppedDat.diviRowVector(stateSTDs);
        FloatMatrix lowDimData = preppedDat.mmul(evecs.getColumns(transformPCAComponents));
        List<FloatMatrix> splitLowDimData = lowDimData.rowsAsList(); // Each data point is a list entry in a FloatMatrix
        // Lambda mapping the list FloatMatrix's to float[]'s
        return splitLowDimData.stream().map(floatmat -> floatmat.data).collect(Collectors.toList());
    }

    @Override
    public List<State> untransform(List<float[]> transformedStates) {
        FloatMatrix lowDimData = new FloatMatrix(transformedStates.size(), transformedStates.get(0).length);
        for (int i = 0; i < transformedStates.size(); i++) {
            for (int j = 0; j < transformedStates.get(0).length; j++) {
                lowDimData.put(i, j, transformedStates.get(i)[j]);
            }
        }

        FloatMatrix restoredDimData = lowDimData.mmul(evecs.getColumns(transformPCAComponents).transpose());

        restoredDimData.muliRowVector(stateSTDs);
        restoredDimData.addiRowVector(stateAvgs);

        List<FloatMatrix> splitRowRestoredDimData = restoredDimData.rowsAsList(); // Each data point is a list entry
		// in a FloatMatrix

        // Lambda mapping the list FloatMatrix's to states's
        return splitRowRestoredDimData.stream().map(floatmat -> new State(floatmat.data)).collect(Collectors.toList());
    }

    @Override
    public List<State> compressAndDecompress(List<State> originalStates) {
        return untransform(transform(originalStates));
    }

    /**
     * Unpack the state data from the nodes, pulling only the stuff we want.
     * Subtract mean, make unit variance.
     **/
    private FloatMatrix unpackData(List<State> states) {

        FloatMatrix dat = new FloatMatrix(states.size(), numStates);

        // Iterate through all nodes
        for (int i = 0; i < states.size(); i++) {
            int colCounter = 0;
            // Through all body parts...
            float bodyX = states.get(i).getStateVarFromName(State.ObjectName.BODY, State.StateName.X);
            for (State.ObjectName obj : State.ObjectName.values()) {
                // For each state of each body part.
                for (State.StateName st : State.StateName.values()) {
                    if (State.StateName.X == st) {
                        dat.put(i, colCounter, states.get(i).getStateVarFromName(obj, st) - bodyX);
                    } else {
                        dat.put(i, colCounter, states.get(i).getStateVarFromName(obj, st));
                    }
                    colCounter++;
                }
            }
        }
        return dat;
    }

    /**
     * Subtracts the mean for each variable, and converts to unit variance.
     * Samples are rows, variables are columns. Alters matrix dat in place.
     */
    private FloatMatrix conditionData(FloatMatrix dat) {
        for (int i = 0; i < dat.columns; i++) {
            // Calculate the mean of a column.
            float sum = 0;
            for (int j = 0; j < dat.rows; j++) {
                sum += dat.get(j, i);
            }
            // Subtract the mean out.
            float avg = sum / dat.rows;
            stateAvgs.put(0, i, avg); // Hold on to the averages for later transformations after the main calculation
			// is done.
            for (int j = 0; j < dat.rows; j++) {
                float centered = dat.get(j, i) - avg;
                dat.put(j, i, centered);
            }
            // Find the standard deviation for each column.
            sum = 0;
            for (int j = 0; j < dat.rows; j++) {
                sum += dat.get(j, i) * dat.get(j, i);
            }
            float std = (float) Math.sqrt(sum / (dat.rows - 1));
            std = (std == 0) ? Float.MIN_NORMAL : std;

            stateSTDs.put(0, i, std); // Keep the STD of each state variable for later transformations.
            // Divide the standard deviation out.
            for (int j = 0; j < dat.rows; j++) {
                float unitVar = dat.get(j, i) / std;
                dat.put(j, i, unitVar);
            }
        }
        return dat;
    }

    @Override
    public int getOutputStateSize() {
        return transformPCAComponents.length;
    }

    @Override
    public String getName() {
        return "PCA " + getOutputStateSize();
    }
}
