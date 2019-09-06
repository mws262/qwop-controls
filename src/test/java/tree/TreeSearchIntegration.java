package tree;

import controllers.Controller_Null;
import controllers.Controller_Random;
import controllers.Controller_ValueFunction;
import controllers.IController;
import distributions.Distribution_Equal;
import game.action.*;
import game.qwop.CommandQWOP;
import game.qwop.GameQWOP;
import game.qwop.StateQWOP;
import game.state.IState;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import savers.DataSaver_Null;
import tree.node.NodeGameExplorable;
import tree.node.evaluator.*;
import tree.sampler.*;
import tree.sampler.rollout.*;
import tree.stage.*;
import value.IValueFunction;
import value.ValueFunction_Constant;
import value.updaters.*;

import java.util.*;

public class TreeSearchIntegration {

    @Test
    public void searchIntegration() {

        StateQWOP sqDistState = GameQWOP.getInitialState();
        Action<CommandQWOP> nullAction = new Action<>(3, CommandQWOP.QP);

        ActionList<CommandQWOP> alist1 = ActionList.makeActionList(new int[] {1, 2, 3, 4}, CommandQWOP.O,
                new Distribution_Equal<>()); // TODO also make distributions be Pickers
        ActionList<CommandQWOP> alist2 = ActionList.makeActionList(new int[] {5, 6, 7, 8}, CommandQWOP.P,
                new Distribution_Equal<>()); // TODO also make distributions be Pickers

        SamplerPicker<CommandQWOP, StateQWOP> samplerQWOP = new SamplerPicker<>(sqDistState, nullAction, alist1,
                alist2);
        for (int i = 0; i < 20; i++) {
            ISampler<CommandQWOP, StateQWOP> current = samplerQWOP.getCurrent();
            TreeWorker<CommandQWOP, StateQWOP> tw = new TreeWorker<>(new GameQWOP(), current, new DataSaver_Null<>());
            TreeStage<CommandQWOP, StateQWOP> ts = new TreeStage_FixedGames<>(10);

            ActionList<CommandQWOP> al = new ActionList<>(new Distribution_Equal<>());
            al.add(new Action<>(10, CommandQWOP.QP));
            al.add(new Action<>(40, CommandQWOP.WP));

            NodeGameExplorable<CommandQWOP, StateQWOP> root = new NodeGameExplorable<>(GameQWOP.getInitialState(), new ActionGenerator_FixedActions<>(al));
            List<TreeWorker<CommandQWOP, StateQWOP>> worker = new ArrayList<>();
            worker.add(tw);
            ts.initialize(worker, root);
            System.out.println("Did a thing!");
            advancePickers(samplerQWOP);
        }
    }

    public static class TreeStagePicker<C extends Command<?>, S extends IState> implements Picker<TreeStage<C, S>> {

        private int idx = 0;

        @Override
        public TreeStage<C, S> getCurrent() {

            TreeStage<C, S> selection;

            switch (idx) {
                case 0:
                    selection = new TreeStage_Dummy<>();
                    break;
                case 1:
                    selection = new TreeStage_FixedGames<>(5);
                    break;
                case 2:
                    selection = new TreeStage_MaxDepth<>(3, 10);
                    break;
                case 3:
                    selection = new TreeStage_MinDepth<>(2);
                    break;
                case 4:
                    selection = new TreeStage_SearchForever<>();
                    break;
//                case 5: // TODO
//                    selection = new TreeStage_ValueFunctionUpdate<>();
//                    break;
//                case 6:
//                    selection = new TreeStage_Grouping<>();
//                    break;
                default:
                    throw new IndexOutOfBoundsException("bad here.");
            }
            return selection;
        }

        @Override
        public void advance() {
            idx++;
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

    /**
     * Samplers
     */
    public static class SamplerPicker<C extends Command<?>, S extends IState> implements Picker<ISampler<C, S>>{
        private int idx = 0;

        private EvaluationFunctionPicker<C, S> evaluationFunctionPicker;
        private RolloutPolicyPicker<C, S> rolloutPolicyPicker;
        private ValueUpdaterPicker<C, S> valueUpdaterPicker = new ValueUpdaterPicker<>();

        private ISampler<C, S> samplerCurrent = getNext();

        public SamplerPicker(S sqDistState, Action<C> nullAction, ActionList<C> alist1, ActionList<C> alist2) {
            evaluationFunctionPicker = new EvaluationFunctionPicker<>(sqDistState);
            rolloutPolicyPicker = new RolloutPolicyPicker<>(sqDistState, nullAction, alist1, alist2);
        }

        public ISampler<C, S> getNext() {
            ISampler<C, S> selection;
            System.out.println("Getting a sampler!");
            switch (idx) {
                case 0:
                    selection = new Sampler_Random<>();
                    System.out.println("Sampler_Random");
                    idx++;
                    break;
                case 1:
                    selection = new Sampler_Deterministic<>();
                    System.out.println("Sampler_Deterministic");

                    idx++;
                    break;
                case 2:
                    selection = new Sampler_Distribution<>();
                    System.out.println("Sampler_Distribution");

                    idx++;
                    break;
                case 3:
                    selection = new Sampler_Greedy<>(evaluationFunctionPicker.getCurrent());
                    System.out.println("Sampler_Greedy");

                    if (advancePickers(evaluationFunctionPicker)) {
                        idx++;
                    }
                    break;
                case 4:
                    selection = new Sampler_UCB<>(evaluationFunctionPicker.getCurrent(), rolloutPolicyPicker.getCurrent(),
                            valueUpdaterPicker.getCurrent(), 1, 1);
                    System.out.println("Sampler_UCB");

                    if (advancePickers(valueUpdaterPicker, rolloutPolicyPicker, evaluationFunctionPicker)) {
                        idx++;
                    }
                    break;
                default:
                    throw new IllegalStateException("Out of samplers.");
            }

            return selection;
        }

        @Override
        public ISampler<C, S> getCurrent() {
            return samplerCurrent;
        }

        @Override
        public void advance() {
            samplerCurrent = getNext();
        }

        public boolean hasNext() {
            return idx < 5;
        }

        public void reset() {
            idx = 0;
        }
    }

    /**
     * Evaluation functions
     */
    public static class EvaluationFunctionPicker<C extends Command<?>, S extends IState> implements Picker<IEvaluationFunction<C, S>> {
        private int idx = 0;

        private S sqDistState;

        EvaluationFunctionPicker(@NotNull S sqDistState) {
            this.sqDistState = Objects.requireNonNull(sqDistState);
        }

        @Override
        public IEvaluationFunction<C, S> getCurrent() {
            System.out.println("Getting an evaluator!");

            IEvaluationFunction<C, S> selection;
            switch(idx) {
                case 0:
                    selection = new EvaluationFunction_Constant<>(5f);
                    break;
                case 1:
                    selection = new EvaluationFunction_Distance<>();
                    break;
                case 2:
                    selection = new EvaluationFunction_Random<>();
                    break;
                case 3:
                    selection = new EvaluationFunction_HandTunedOnState<>();
                    break;
                case 4:
                    selection = new EvaluationFunction_SqDistFromOther<>(sqDistState);
                    break;
                case 5:
                    selection = new EvaluationFunction_Velocity<>();
                    break;
                default:
                    throw new IllegalStateException("index out of bounds.");
            }
            return selection;
        }

        @Override
        public void advance() {
            idx++;
        }

        public boolean hasNext() {
            return idx < 4;
        }

        public void reset() {
            idx = 0;
        }
    }

    /**
     * Rollout policies
     */
    public static class RolloutPolicyPicker<C extends Command<?>, S extends IState> implements Picker<IRolloutPolicy<C, S>> {
        private int idx = 0;

        private EvaluationFunctionPicker<C, S> evaluationFunctionPicker;
        private ActionGeneratorPicker<C> actionGeneratorPicker;
        private ControllerPicker<C, S> controllerPicker;
        private IRolloutPolicy<C, S> rolloutPolicyCurrent;

        RolloutPolicyPicker(S comparisonState, Action<C> nullAction, ActionList<C> alist1, ActionList<C> alist2) {
            evaluationFunctionPicker = new EvaluationFunctionPicker<>(comparisonState);
            controllerPicker = new ControllerPicker<>(nullAction);
            actionGeneratorPicker = new ActionGeneratorPicker<>(alist1, alist2);
        }

        private IRolloutPolicy<C, S> getNext() {
            System.out.println("Getting a rolloutpolicy!");

            IRolloutPolicy<C, S> selection;

            switch(idx) {
                case 0:
                    selection = new RolloutPolicy_JustEvaluate<>(evaluationFunctionPicker.getCurrent());
                    if (advancePickers(evaluationFunctionPicker)) {
                        idx++;
                    }
                    break;
                case 1:
                    selection = new RolloutPolicy_EndScore<>(evaluationFunctionPicker.getCurrent(), actionGeneratorPicker.getCurrent(),
                            controllerPicker.getCurrent());
                    if (advancePickers(controllerPicker, actionGeneratorPicker, evaluationFunctionPicker)) {
                        idx++;
                    }
                    break;
                case 2:
                    selection = new RolloutPolicy_DecayingHorizon<>(evaluationFunctionPicker.getCurrent(),
                            actionGeneratorPicker.getCurrent(), controllerPicker.getCurrent(), 25);
                    if (advancePickers(controllerPicker, actionGeneratorPicker, evaluationFunctionPicker)) {
                        idx++;
                    }
                    break;
                case 3:
                    selection = new RolloutPolicy_DeltaScore<>(evaluationFunctionPicker.getCurrent(),
                            actionGeneratorPicker.getCurrent(), controllerPicker.getCurrent());
                    if (advancePickers(actionGeneratorPicker, evaluationFunctionPicker)) {
                        idx++;
                    }
                    break;
                case 4:
                    selection = new RolloutPolicy_EntireRun<>(actionGeneratorPicker.getCurrent(), controllerPicker.getCurrent());
                    if (advancePickers(controllerPicker, actionGeneratorPicker)) {
                        idx++;
                    }
                    break;
                case 5:
                    selection = new RolloutPolicy_RandomColdStart<>(evaluationFunctionPicker.getCurrent(),
                            actionGeneratorPicker.getCurrent());
                    if (advancePickers(controllerPicker, actionGeneratorPicker)) {
                        idx++;
                    }
                    break;
//                case 6:
//                    selection = new RolloutPolicy_Window<>(); // TODO
//                    break;
//                case 7:
//                    selection = new RolloutPolicy_WeightWithValueFunction<>(evalFunRolloutChosen);
//                    break;
                default:
                    throw new IllegalStateException("index out of bounds.");
            }
            return selection;
        }

        @Override
        public IRolloutPolicy<C, S> getCurrent() {
            if (rolloutPolicyCurrent == null) {
                rolloutPolicyCurrent = getNext();
            }
            return rolloutPolicyCurrent;
        }

        @Override
        public void advance() {
            rolloutPolicyCurrent = getNext();
        }

        @Override
        public boolean hasNext() {
            return idx < 6;
        }

        @Override
        public void reset() {
            idx = 0;
        }
    }

    /**
     * Value updaters
     */
    public static class ValueUpdaterPicker<C extends Command<?>, S extends IState> implements Picker<IValueUpdater<C, S>> {

        private int idx = 0;

        @Override
        public IValueUpdater<C, S> getCurrent() {
            System.out.println("Getting a valueUpdater!");

            IValueUpdater<C, S> selection;
            switch (idx) {
                case 0:
                    selection = new ValueUpdater_Average<>();
                    break;
                case 1:
                    selection = new ValueUpdater_HardSet<>();
                    break;
                case 2:
                    selection = new ValueUpdater_TopNChildren<>(3);
                    break;
                case 3:
                    selection = new ValueUpdater_TopWindow<>(3);
                    break;
                case 4:
                    selection = new ValueUpdater_StdDev<>(1);
                    break;
                default:
                    throw new IndexOutOfBoundsException("Bad here.");
            }
            return selection;
        }

        @Override
        public void advance() {
            idx++;
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

    /**
     * Action generators
     */
    public static class ActionGeneratorPicker<C extends Command<?>> implements Picker<IActionGenerator<C>> {

        private int idx = 0;

        private ActionList<C> alist1;
        private ActionList<C> alist2;

        public ActionGeneratorPicker(ActionList<C> alist1, ActionList<C> alist2) {
            this.alist1 = alist1;
            this.alist2 = alist2;
        }

        @Override
        public IActionGenerator<C> getCurrent() {
            System.out.println("Getting an action generator!");

            IActionGenerator<C> selection;
            switch(idx) {
                case 0:
                    selection = new ActionGenerator_UniformNoRepeats<>(alist1, alist2);
                    idx++;
                    break;
                case 1:
                    selection = new ActionGenerator_Null<>();
                    idx++;
                    break;
                case 2:
                    selection = new ActionGenerator_FixedActions<>(alist1);
                    idx++;
                    break;
                case 3:
                    selection = new ActionGenerator_FixedSequence<>(alist1, alist2);
                    idx++;
                    break;
                default:
                    throw new IndexOutOfBoundsException("Bad here.");
            }
            return selection;
        }

        @Override
        public void advance() {
            idx++;
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

    /**
     * Controllers
     */
    public static class ControllerPicker<C extends Command<?>, S extends IState> implements Picker<IController<C, S>> {
        private int idx = 0;

        private Action<C> nullAction;

        private ValueFunctionPicker<C, S> valueFunctionPicker;

        public ControllerPicker(Action<C> nullAction) {
            this.nullAction = nullAction;
            valueFunctionPicker = new ValueFunctionPicker<>(0.2f, nullAction.getCommand());
        }

        @Override
        public IController<C, S> getCurrent() {
            System.out.println("Getting a controller!");

            IController<C, S> selection;
            switch(idx) {
                case 0:
                    selection = new Controller_Null<>(nullAction);
                    break;
                case 1:
                    selection = new Controller_Random<>();
                    break;
                case 2:
                    selection = new Controller_ValueFunction<>(valueFunctionPicker.getCurrent());
                    if (advancePickers(valueFunctionPicker)) {
                        idx++;
                    }
                    break;
                default:
                    throw new IndexOutOfBoundsException("Bad here.");
            }
            return selection;
        }

        @Override
        public void advance() {
            idx++;
        }

        @Override
        public boolean hasNext() {
            return idx < 3;
        }

        @Override
        public void reset() {
            idx = 0;
        }
    }

    /**
     * Value functions.
     */
    public static class ValueFunctionPicker<C extends Command<?>, S extends IState> implements Picker<IValueFunction<C, S>> {

        private IValueFunction<C, S> constValFun;

        private int idx = 0;

        public ValueFunctionPicker(float constVal, C constCommand) {
            constValFun = new ValueFunction_Constant<>(constVal, constCommand);
        }

        @Override
        public IValueFunction<C, S> getCurrent() {
            System.out.println("Getting a value function!");

            switch(idx) {
                case 0:
                    return constValFun;
                default:
                    throw new IndexOutOfBoundsException("Bad here.");
            }
//            new ValueFunction_TensorFlow<>(); // TODO currently only for QWOP commands.
//            new ValueFunction_TensorFlow_StateOnly<>()
        }

        @Override
        public void advance() {
            idx++;
        }

        @Override
        public boolean hasNext() {
            return idx < 1;
        }

        @Override
        public void reset() {
            idx = 0;
        }
    }

    private static boolean advancePickers(Picker<?>... pickers) {
        return advancePickers(new LinkedList<>(Arrays.asList(pickers)));
    }

    // returns true if nothing more to pick.
    private static boolean advancePickers(Queue<Picker<?>> pickers) {
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

    public interface Picker<T> {
        T getCurrent();
        void advance();
        boolean hasNext();
        void reset();
    }
}

