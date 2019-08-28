package game.state.transform;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import game.state.IState;
import org.jblas.FloatMatrix;
import org.jblas.Singular;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Transform_PCA<S extends IState> implements ITransform<S> {

    /**
     * Eigenvectors found during SVD of the conditioned states. They represent the principle directions that explain
     * most of the state variance.
     */
    private FloatMatrix eigenvectors;

    /**
     * Averages of the states of the data given to calculate the PCA.
     */
    private FloatMatrix stateAvgs; // = new FloatMatrix(1, numStates);

    /**
     * Standard deviations of the states of the data given to calculate the PCA.
     */
    private FloatMatrix stateSTDs; // = new FloatMatrix(1, numStates);

    /**
     * PCA components used to do transforms. Usually will only be the first couple.
     */
    @JsonSerialize(using = Transform_PCA.Serializer.class)
    @JsonDeserialize(using = Transform_PCA.Deserializer.class)
    public final int[] transformPCAComponents;

    /**
     * Make a new PCA transformer. Note that it will do the full PCA, but will only used
     * the specified components for future transforms. In this case, that means that the
     * reduced state will have transformPCAComponents.length components.
     *
     * @param transformPCAComponents The PCA components that will be used for the transform.
     */
    public Transform_PCA(@JsonProperty("transformPCAComponents") int[] transformPCAComponents) {
        this.transformPCAComponents = transformPCAComponents;
    }

    @Override
    public void updateTransform(List<S> nodesToUpdateFrom) {
        // Do PCA!
        FloatMatrix dataSet = conditionData(unpackData(nodesToUpdateFrom));
        FloatMatrix[] USV = Singular.fullSVD(dataSet);
        eigenvectors = USV[2];

        /* During SVD we find the eigenvalues, the weights for what portion of variance is explained by the
         * corresponding eigenvector. */
        FloatMatrix eigenvalues = USV[1].mul(USV[1]).div(dataSet.rows);

        // Also make the vector of normalized eigenvalues.
        float evalSum = 0;
        for (int i = 0; i < eigenvalues.length; i++) {
            evalSum += eigenvalues.get(i);
        }
        // Normalize so the sum of the eigenvalues == 1
        FloatMatrix evalsNormalized = new FloatMatrix(eigenvalues.length);
        for (int i = 0; i < eigenvalues.length; i++) {
            evalsNormalized.put(i, eigenvalues.get(i) / evalSum);
        }
    }

    @Override
    public List<float[]> transform(List<S> originalStates) {
        FloatMatrix preppedDat = unpackData(originalStates);
        preppedDat.subiRowVector(stateAvgs);
        preppedDat.diviRowVector(stateSTDs);
        FloatMatrix lowDimData = preppedDat.mmul(eigenvectors.getColumns(transformPCAComponents));
        List<FloatMatrix> splitLowDimData = lowDimData.rowsAsList(); // Each data point is a list entry in a FloatMatrix
        // Lambda mapping the list FloatMatrix's to float[]'s
        return splitLowDimData.stream().map(floatmat -> floatmat.data).collect(Collectors.toList());
    }

    @Override
    public float[] transform(S originalState) {
        // This single-state transform method was added later, so I'm just going to make a list and send it over to
        // the existing method.
        List<S> stateList = new ArrayList<>();
        stateList.add(originalState);
        return transform(stateList).get(0);
    }

    @Override
    public List<float[]> untransform(List<float[]> transformedStates) {
        FloatMatrix lowDimData = new FloatMatrix(transformedStates.size(), transformedStates.get(0).length);
        for (int i = 0; i < transformedStates.size(); i++) {
            for (int j = 0; j < transformedStates.get(0).length; j++) {
                lowDimData.put(i, j, transformedStates.get(i)[j]);
            }
        }
        FloatMatrix restoredDimData = lowDimData.mmul(eigenvectors.getColumns(transformPCAComponents).transpose());

        restoredDimData.muliRowVector(stateSTDs);
        restoredDimData.addiRowVector(stateAvgs);

        List<FloatMatrix> splitRowRestoredDimData = restoredDimData.rowsAsList(); // Each data point is a list entry
		// in a FloatMatrix

        return splitRowRestoredDimData.stream().map(matrix -> matrix.data).collect(Collectors.toList());
    }

    @Override
    public List<float[]> compressAndDecompress(List<S> originalStates) {
        return untransform(transform(originalStates));
    }

    private FloatMatrix unpackData(List<S> states) {

        float[][] flatStates = new float[states.size()][];

        for (int i = 0; i < states.size(); i++) {
            flatStates[i] = states.get(i).flattenState();
        }
        return new FloatMatrix(flatStates);
    }

    /**
     * Subtracts the mean for each variable, and converts to unit variance.
     * Samples are rows, variables are columns. Alters matrix dat in place.
     */
    private FloatMatrix conditionData(FloatMatrix dat) {

        if (stateAvgs == null) {
            stateAvgs = new FloatMatrix(1, dat.columns);
        }
        if (stateSTDs == null) {
            stateSTDs = new FloatMatrix(1, dat.columns);
        }

        for (int i = 0; i < dat.columns; i++) {
            // Calculate the mean of a column.
            float sum = 0;
            for (int j = 0; j < dat.rows; j++) {
                sum += dat.get(j, i);
            }
            // Subtract the mean out.
            float avg = sum / dat.rows;
            stateAvgs.put(0, i, avg); // Hold on to the averages for later game.state.transformations after the goals calculation
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

            stateSTDs.put(0, i, std); // Keep the STD of each state variable for later game.state.transformations.
            // Divide the standard deviation out.
            for (int j = 0; j < dat.rows; j++) {
                float unitVar = dat.get(j, i) / std;
                dat.put(j, i, unitVar);
            }
        }
        return dat;
    }

    @Override
    public int getOutputSize() {
        return transformPCAComponents.length;
    }

    @Override
    public String getName() {
        return "PCA " + getOutputSize();
    }

    public static class Deserializer extends StdDeserializer<Object> {

        @SuppressWarnings("unused")
        public Deserializer() {
            this(null);
        }

        public Deserializer(Class<Object> t) {
            super(t);
        }

        @Override
        public int[] deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);

            String durationList = node.iterator().next().asText();
            String[] durations = durationList.split(" ");
            int[] components = new int[durations.length];

            for (int i = 0; i < durations.length; i++)  {
                components[i] = Integer.parseInt(durations[i]);
            }

            return components;
        }
    }

    public static class Serializer extends StdSerializer<Object> {

        @SuppressWarnings("unused")
        public Serializer() {
            this(null);
        }

        public Serializer(Class<Object> t) {
            super(t);
        }

        @Override
        public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider) throws IOException {

            StringBuilder sb = new StringBuilder();
            for (int component : (int[]) value) {
                sb.append(component).append(" ");
            }
            jgen.writeStartObject();
            jgen.writeStringField("transformPCAComponents", sb.toString());
            jgen.writeEndObject();
        }
    }
}
