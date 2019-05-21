package evaluators;

import tree.NodeQWOPBase;

import java.util.Objects;

/**
 * Implementation of a node evaluation function which is a hand-tuned combination of state information, having to do
 * with torso angle, x position, and x velocity.
 *
 * @author matt
 */
@SuppressWarnings("unused")
public class EvaluationFunction_HandTunedOnState implements IEvaluationFunction {

    @Override
    public float getValue(NodeQWOPBase<?> nodeToEvaluate) {
        Objects.requireNonNull(nodeToEvaluate.getState());

        float value = 0.f;
        value += getAngleValue(nodeToEvaluate);
        value += getDistanceValue(nodeToEvaluate);
        value += getVelocityValue(nodeToEvaluate);

        return value;
    }

    @Override
    public String getValueString(NodeQWOPBase<?> nodeToEvaluate) {
        Objects.requireNonNull(nodeToEvaluate.getState());

        String value = "";
        value += "Angle value: ";
        value += getAngleValue(nodeToEvaluate);
        value += ", Distance value: ";
        value += getDistanceValue(nodeToEvaluate);
        value += ", Velocity value: ";
        value += getVelocityValue(nodeToEvaluate);
        return value;
    }

    /**
     * Value component based on angles.
     *
     * @param nodeToEvaluate Node being scored.
     * @return A scalar value associated with state angles.
     */
    private float getAngleValue(NodeQWOPBase<?> nodeToEvaluate) {
        return nodeToEvaluate.getState().body.getTh();
    }

    /**
     * Value component based on x distances.
     *
     * @param nodeToEvaluate Node being scored.
     * @return A scalar value associated with horizontal positions.
     */
    private float getDistanceValue(NodeQWOPBase<?> nodeToEvaluate) {
        return nodeToEvaluate.getState().body.getX();
    }

    /**
     * Value component based on velocities.
     *
     * @param nodeToEvaluate Node being scored.
     * @return A scalar value associated with state velocities.
     */
    private float getVelocityValue(NodeQWOPBase<?> nodeToEvaluate) {
        return nodeToEvaluate.getState().body.getDx();
    }

    @Override
    public EvaluationFunction_HandTunedOnState getCopy() {
        return new EvaluationFunction_HandTunedOnState();
    }
}
