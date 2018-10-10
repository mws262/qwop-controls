package main;

/**
 * Generic evaluation of a node based on any factors. Going with "higher-is-better" interpretation of evaluation.
 * Primarily used by ISampler
 *
 * @author Matt
 */
public interface IEvaluationFunction {

    /**
     * Get the evaluated objective value of nodeToEvaluate. Higher is better.
     **/
    float getValue(Node nodeToEvaluate);

    /**
     * Get a formatted string of the evaluation of a note. Typically this will divide the value up into whatever
     * components go into it.
     **/
    String getValueString(Node nodeToEvaluate);

    /**
     * Get a copy of this.
     **/
    IEvaluationFunction clone();
}
