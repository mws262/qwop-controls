package tree;

import controllers.Controller_Null;
import controllers.Controller_Random;
import controllers.Controller_ValueFunction;
import controllers.IController;
import game.action.*;
import game.state.IState;
import tree.node.evaluator.*;
import tree.sampler.*;
import tree.sampler.rollout.*;
import value.updaters.IValueUpdater;

import java.util.Queue;

public class TreeSearchIntegration {

    public TreeSearchIntegration() {

    }

    public class Search<C extends Command<?>, S extends IState> {




        IEvaluationFunction<C, S> evalFunRolloutChosen = evalFunRollout0; // TODO;

        IActionGenerator<C> actionGeneratorRollout0 = new ActionGenerator_Null<>();
        IActionGenerator<C> actionGeneratorRollout1 = new ActionGenerator_UniformNoRepeats<>();
        IActionGenerator<C> actionGeneratorRollout2 = new ActionGenerator_FixedActions<>();
        IActionGenerator<C> actionGeneratorRollout3 = new ActionGenerator_FixedSequence<>();

        IActionGenerator<C> actiongeneratorRolloutChosen = actionGeneratorRollout0;

        IController<C, S> controllerRollout0 = new Controller_Null<>();
        IController<C, S> controllerRollout1 = new Controller_Random<>();
        IController<C, S> controllerRollout2 = new Controller_ValueFunction<>();

        IController<C, S> controllerRolloutChosen = controllerRollout0;



        ISampler<C, S> sampler0 = new Sampler_Random<>();
        ISampler<C, S> sampler1 = new Sampler_Deterministic<>();
        ISampler<C, S> sampler2 = new Sampler_Distribution<>();
        ISampler<C, S> sampler3 = new Sampler_FixedDepth<>(3);
        ISampler<C, S> sampler4 = new Sampler_Greedy<>(evalFunChosen);
        ISampler<C, S> sampler5 = new Sampler_UCB<>(evalFunChosen, rolloutPolicyChosen);

    }

    public static class SamplerPicker<C extends Command<?>, S extends IState> {
        private int idx = 0;

        private EvaluationFunctionPicker<C, S> evaluationFunctionPicker = new EvaluationFunctionPicker<>();
        private RolloutPolicyPicker<C, S> rolloutPolicyPicker = new RolloutPolicyPicker<>();
        private ValueUpdaterPicker<C, S> valueUpdaterPicker = new ValueUpdaterPicker<>();

        private IEvaluationFunction<C, S> evaluationFunction = evaluationFunctionPicker.getNext();
        private IRolloutPolicy<C, S> rolloutPolicy = rolloutPolicyPicker.getNext();
        private IValueUpdater<C, S> valueUpdater = valueUpdaterPicker.getCurrent();

        public ISampler<C, S> getNext() {
            ISampler<C, S> selection = null;
            switch(idx) {
                case 0:
                    selection = new Sampler_Random<>();
                    idx++;
                    break;
                case 1:
                    selection = new Sampler_Deterministic<>();
                    idx++;
                    break;
                case 2:
                    selection = new Sampler_Distribution<>();
                    idx++;
                    break;
                case 3:
                    selection = new Sampler_FixedDepth<>(3);
                    idx++;
                    break;
                case 4:
                    selection = new Sampler_Greedy<>(evaluationFunction);
                    if (evaluationFunctionPicker.hasNext()) {
                        evaluationFunction = evaluationFunctionPicker.getNext();
                    } else {
                        evaluationFunctionPicker.reset();
                        idx++;
                    }
                case 5:
                    selection = new Sampler_UCB<>(evaluationFunctionPicker.getNext(), rolloutPolicy, valueUpdater, 1,
                            1);

                    if (valueUpdaterPicker.hasNext()) {
                        valueUpdater = valueUpdaterPicker.getCurrent();
                    } else {
                        valueUpdaterPicker.reset();

                        if (rolloutPolicyPicker.hasNext()) {
                            rolloutPolicy = rolloutPolicyPicker.getNext();
                        } else {
                            rolloutPolicyPicker.reset();

                            if (evaluationFunctionPicker.hasNext()) {
                                evaluationFunction = evaluationFunctionPicker.getNext();
                            } else {
                                evaluationFunctionPicker.reset();
                                idx++;
                            }
                        }
                    }


                default:
                    throw new IllegalStateException("Out of samplers.");
            }

            return selection;
        }

        public boolean hasNext() {
            return idx < 6;
        }

        public void reset() {
            idx = 0;
        }
    }

    public static class EvaluationFunctionPicker<C extends Command<?>, S extends IState> {
        private int idx = 0;

        private S sqDistState;

        public EvaluationFunctionPicker(S sqDistState) {
            this.sqDistState = sqDistState;
        }

        public IEvaluationFunction<C, S> getNext() {
            IEvaluationFunction<C, S> selection = null;

            switch(idx) {
                case 0:
                    selection = new EvaluationFunction_Constant<>(5f);
                    idx++;
                    break;
                case 1:
                    selection = new EvaluationFunction_Distance<>();
                    idx++;
                    break;
                case 2:
                    selection = new EvaluationFunction_Random<>();
                    idx++;
                    break;
                case 3:
                    selection = new EvaluationFunction_HandTunedOnState<>();
                    break;
                case 4:
                    selection = new EvaluationFunction_SqDistFromOther<>(sqDistState);
                    idx++;
                    break;
                case 5:
                    selection = new EvaluationFunction_Velocity<>();
                    idx++;
                    break;
                default:
                    throw new IllegalStateException("index out of bounds.");
            }
            return selection;
        }

        public boolean hasNext() {
            return idx < 6;
        }
        public void reset() {
            idx = 0;
        }
    }

    public static class RolloutPolicyPicker<C extends Command<?>, S extends IState> implements Picker<RolloutPolicyPicker<C, S>> {
        private int idx = 0;

        private EvaluationFunctionPicker<C, S> evaluationFunctionPicker = new EvaluationFunctionPicker<>();
        private RolloutPolicyPicker<C, S> rolloutPolicyPicker = new RolloutPolicyPicker<>();
        private ValueUpdaterPicker<C, S> valueUpdaterPicker = new ValueUpdaterPicker<>();
        private ActionGeneratorPicker<C> actionGeneratorPicker = new ActionGeneratorPicker<>();
        private ControllerPicker<C, S> controllerPicker = new ControllerPicker<>();

        private IEvaluationFunction<C, S> evaluationFunction = evaluationFunctionPicker.getNext();
        private IRolloutPolicy<C, S> rolloutPolicy = rolloutPolicyPicker.getNext();
        private IActionGenerator<C> actionGenerator = actionGeneratorPicker.getCurrent();
        private IController<C, S> controller = controllerPicker.getCurrent();

        public IRolloutPolicy<C, S> getNext() {
            IRolloutPolicy<C, S> selection = null;

            switch(idx) {
                case 0:
                    selection = new RolloutPolicy_JustEvaluate<>(evaluationFunction);
                    break;
                case 1:
                    selection = new RolloutPolicy_EndScore<>(evaluationFunction, actionGenerator, controller);
                    advanceControlGenEval();
                    break;
                case 2:
                    selection = new RolloutPolicy_DecayingHorizon<>(evaluationFunction, actionGenerator, controller, 25);
                    advanceControlGenEval();
                    break;
                case 3:
                    selection = new RolloutPolicy_DeltaScore<>(evaluationFunction, actionGenerator,
                            controller);
                    advanceControlGenEval();
                    break;
                case 4:
                    selection = new RolloutPolicy_EntireRun<>(actionGenerator, controller);
                    break;
                case 5:
                    selection = new RolloutPolicy_RandomColdStart<>(evalFunRolloutChosen, actiongeneratorRolloutChosen);
                    break;
                case 6:
                    selection = new RolloutPolicy_Window<>();
                    break;
                case 7:
                    selection = new RolloutPolicy_WeightWithValueFunction<>(evalFunRolloutChosen);
                    break;
                default:
                    throw new IllegalStateException("index out of bounds.");
            }
            return selection;
        }

        @Override
        public RolloutPolicyPicker<C, S> getCurrent() {
            return null;
        }

        @Override
        public void advance() {

        }

        @Override
        public boolean hasNext() {
            return idx < 8;
        }

        @Override
        public void reset() {
            idx = 0;
        }
    }

    static boolean advancePickers(Queue<Picker<?>> pickers) {
        if (pickers.isEmpty()) {
            throw new IllegalArgumentException("Pickers should not be empty here.");
        }

        Picker<?> individual = pickers.poll();

        // If this picker has more options, we need not recurse further. Advance this one and return.
        if (individual.hasNext()) {
            individual.advance();
            return false; // Not completed all possible combinations.
        } else {
            individual.reset();
            if (!pickers.isEmpty()) {
                return advancePickers(pickers);
            } else {
                return true;
            }
        }
    }

    public static class ValueUpdaterPicker<C extends Command<?>, S extends IState> implements Picker<IValueUpdater<C, S>> {
        @Override
        public IValueUpdater<C, S> getCurrent() {
            return null;
        }

        @Override
        public void advance() {

        }

        @Override
        public boolean hasNext() {
            return true;
        }

        @Override
        public void reset() {}
    }

    public static class ActionGeneratorPicker<C extends Command<?>> implements Picker<IActionGenerator<C>> {

        private IActionGenerator<C> current;
        private int idx = 0;

        @Override
        public IActionGenerator<C> getCurrent() {
            return current;
        }

        @Override
        public void advance() {
            switch(idx) {
                case 0:
                    current = new ActionGenerator_Null<>();
                    break;
                case 1:
                    current = new ActionGenerator_UniformNoRepeats<>();
                    break;
                case 2:
                    current = new ActionGenerator_FixedActions<>();
                    break;
                case 3:
                    current = new ActionGenerator_FixedSequence<>();
                    break;
                default:
                    throw new IndexOutOfBoundsException("Bad here.");
            }
        }

        @Override
        public boolean hasNext() {
            return idx < 4;
        }

        @Override
        public void reset() {
            idx = 0;
        }
    }

    public static class ControllerPicker<C extends Command<?>, S extends IState> implements Picker<IController<C, S>> {

        @Override
        public IController<C, S> getCurrent() {
            return null;
        }

        @Override
        public void advance() {

        }

        @Override
        public boolean hasNext() {
            return true;
        }

        @Override
        public void reset() {}
    }

    public interface Picker<T> {
        T getCurrent();
        void advance();
        boolean hasNext();
        void reset();
    }
}

