package evaluators;

import main.Node;

/**
 * Implementation of a node evaluation function which is a hand-tuned combination of state information, having to do
 * with torso angle, x position, and x velocity.
 *
 * @author matt
 */
public class EvaluationFunction_HandTunedOnState implements IEvaluationFunction {

    @Override
    public float getValue(Node nodeToEvaluate) {
        if (!nodeToEvaluate.isStateAvailable())
            throw new NullPointerException("Trying to evaluate a node based on state information which has not yet " +
                    "been assigned in that node.");
        float value = 0.f;
        value += getAngleValue(nodeToEvaluate);
        value += getDistanceValue(nodeToEvaluate);
        value += getVelocityValue(nodeToEvaluate);

        return value;
    }

    @Override
    public String getValueString(Node nodeToEvaluate) {
        if (!nodeToEvaluate.isStateAvailable())
            throw new NullPointerException("Trying to evaluate a node based on state information which has not yet " +
                    "been assigned in that node.");
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
    private float getAngleValue(Node nodeToEvaluate) {
        return nodeToEvaluate.getState().body.th;
    }

    /**
     * Value component based on x distances.
     *
     * @param nodeToEvaluate Node being scored.
     * @return A scalar value associated with horizontal positions.
     */
    private float getDistanceValue(Node nodeToEvaluate) {
        return nodeToEvaluate.getState().body.x;
    }

    /**
     * Value component based on velocities.
     *
     * @param nodeToEvaluate Node being scored.
     * @return A scalar value associated with state velocities.
     */
    private float getVelocityValue(Node nodeToEvaluate) {
        return nodeToEvaluate.getState().body.dx;
    }

    @Override
    public EvaluationFunction_HandTunedOnState clone() {
        return new EvaluationFunction_HandTunedOnState();
    }
}
