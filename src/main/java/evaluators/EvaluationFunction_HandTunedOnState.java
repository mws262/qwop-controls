package evaluators;

import tree.INode;

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
    public float getValue(INode nodeToEvaluate) {
        Objects.requireNonNull(nodeToEvaluate.getState());

        float value = 0.f;
        value += getAngleValue(nodeToEvaluate);
        value += getDistanceValue(nodeToEvaluate);
        value += getVelocityValue(nodeToEvaluate);

        return value;
    }

    @Override
    public String getValueString(INode nodeToEvaluate) {
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
    private float getAngleValue(INode nodeToEvaluate) {
        return nodeToEvaluate.getState().torso.getTh();
    }

    /**
     * Value component based on x distances.
     *
     * @param nodeToEvaluate Node being scored.
     * @return A scalar value associated with horizontal positions.
     */
    private float getDistanceValue(INode nodeToEvaluate) {
        return nodeToEvaluate.getState().torso.getX();
    }

    /**
     * Value component based on velocities.
     *
     * @param nodeToEvaluate Node being scored.
     * @return A scalar value associated with state velocities.
     */
    private float getVelocityValue(INode nodeToEvaluate) {
        return nodeToEvaluate.getState().torso.getDx();
    }

    @Override
    public EvaluationFunction_HandTunedOnState getCopy() {
        return new EvaluationFunction_HandTunedOnState();
    }
}
