package controllers;

import data.EvictingTreeMap;
import data.LIFOFixedSize;
import data.TFRecordDataParsers;
import game.GameUnified;
import game.IGameSerializable;
import game.action.Action;
import game.state.IState;
import game.state.IState.ObjectName;
import game.state.State;
import game.state.StateVariable;
import game.state.StateVariable.StateName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tensorflow.example.FeatureList;
import org.tensorflow.example.SequenceExample;
import tree.Utility;
import tree.node.NodeQWOPExplorableBase;
import tree.node.NodeQWOPGraphicsBase;
import ui.runner.PanelRunner;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;

/**
 * QWOP controller which takes known sample trajectories, and tries to pick the best match to use for feedback during
 * a run.
 *
 * @author matt
 */
public class Controller_NearestNeighborApprox implements IController {

    /**
     *
     */
    private ObjectName sortByPart = ObjectName.BODY;
    /**
     *
     */
    private StateName sortBySt = StateName.TH;

    /**
     *
     */
    private boolean penalizeEndOfSequences = false;
    /**
     *
     */
    private float maxPenaltyForEndOfSequence = 50; // Penalty towards choosing runs near the end of their sequences.

    /**
     *
     */
    private boolean comparePreviousStates = true;
    private int numPreviousStatesToCompare = 10;
    private float previousStatePenaltyMult = 2f;

    /**
     *
     */
    private boolean enableVoting = false;

    /**
     *
     */
    private boolean enableTrajectorySnapping = false;
    private float trajectorySnappingThreshold = 1f;

    /**
     *
     */
    private boolean penalizeSlow = false;
    private float penalizeSlowMult = 50;
    private int penalizeSlowHorizon = 50;

    /**
     * How many nearby (in terms of body theta) states are compared when determining the "closest" one.
     */
    private int upperSetLimit = 20000;

    private int lowerSetLimit = 20000;

    /**
     * Total number of states loaded from TFRecord files.
     */
    private int numStatesLoaded = 0;

    /**
     * All states loaded, regardless of run, in sorted order, by body theta.
     */
    public NavigableMap<Float, StateHolder> allStates = new TreeMap<>();

    /**
     * All runs loaded.
     **/
    public Set<RunHolder> runs = new HashSet<>();

    /**
     * Keep track of which total run that the currently selected action comes from.
     **/
    private RunHolder currentTrajectory;
    private StateHolder currentTrajectoryStateMatch;
    private DecisionHolder currentDecision;
    private LIFOFixedSize<IState> previousStatesLIFO = new LIFOFixedSize<>(numPreviousStatesToCompare);

    private boolean[] chosenKeys = new boolean[4];

    //IMPORTANT  DUE TO COLLECTION BUG
    private boolean killFirstTwoActions = true;

    private State weights;

    private static final Logger logger = LogManager.getLogger(Controller_NearestNeighborApprox.class);

    /**
     * Create a trajectory library-type controller by providing a list of files to look through.
     *
     * @param files A list of .TFRecord SequenceExample files from which to load trajectories.
     * @throws IOException Files are not successfully loaded.
     */
    public Controller_NearestNeighborApprox(List<File> files) {
        try {
            loadAll(files);
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.debug(numStatesLoaded + " states were loaded!");

        float[] weightVals = new float[State.STATE_SIZE];
        Arrays.fill(weightVals, 1f);
        weights = new State(weightVals, false);
    }

    @Override
    public Action policy(NodeQWOPExplorableBase<?> state) {
        // Get nearest states (determined ONLY by body theta).
        IState st = state.getState();
        float sortBy = st.getStateVariableFromName(sortByPart).getStateByName(sortBySt);

        NavigableMap<Float, StateHolder> lowerSet = allStates.headMap(sortBy, true);
        NavigableMap<Float, StateHolder> upperSet = allStates.tailMap(sortBy, false);

        EvictingTreeMap<Float, StateHolder> topMatches = new EvictingTreeMap<>(10);

        Utility.tic();
        lowerSet.values().stream().limit(lowerSetLimit).forEach(v -> topMatches.put(totalEvalFunction(v, st), v));
        upperSet.values().stream().limit(upperSetLimit).forEach(v -> topMatches.put(totalEvalFunction(v, st), v));
        Utility.toc();
        Entry<Float, StateHolder> bestEntry = topMatches.firstEntry();
        StateHolder bestMatch = bestEntry.getValue();
        float bestMatchError = bestEntry.getKey();


        // Voting?
        if (enableVoting) {
            Iterator<Float> iter = topMatches.keySet().iterator();
            float keySum = 0;
            float[] keysWeighted = new float[4];

            for (Map.Entry<Float, StateHolder> entry : topMatches.entrySet()) {
                Float k = entry.getKey();
                StateHolder v = entry.getValue();

                float multiplier = 1f / Float.max(k, Float.MIN_VALUE);
                keySum += multiplier;
                keysWeighted[0] += multiplier * (v.keys[0] ? 1 : 0);
                keysWeighted[1] += multiplier * (v.keys[1] ? 1 : 0);
                keysWeighted[2] += multiplier * (v.keys[2] ? 1 : 0);
                keysWeighted[3] += multiplier * (v.keys[3] ? 1 : 0);

                logger.debug(
                        k + ", \t"
                                + v.keys[0] + "," + v.keys[1] + "," + v.keys[2] + "," + v.keys[3]);
            }

            keysWeighted[0] /= keySum;
            keysWeighted[1] /= keySum;
            keysWeighted[2] /= keySum;
            keysWeighted[3] /= keySum;

            chosenKeys[0] = keysWeighted[0] >= 0.5f;
            chosenKeys[1] = keysWeighted[1] >= 0.5f;
            chosenKeys[2] = keysWeighted[2] >= 0.5f;
            chosenKeys[3] = keysWeighted[3] >= 0.5f;

            if (!Arrays.equals(chosenKeys, bestEntry.getValue().keys)) {
                logger.debug("Top match overridden by voting.");
            }
        } else {
            chosenKeys = bestMatch.keys;
        }

        // Choose whether to stay on the previous trajectory or not.
        if (enableTrajectorySnapping && currentTrajectory != null) {
            int lastStIdx = currentTrajectory.states.indexOf(currentTrajectoryStateMatch);

            if (lastStIdx < currentTrajectory.states.size() - 2) {

                StateHolder nextStateOnOldTraj = currentTrajectory.states.get(lastStIdx + 1);
                float oldTrajError = sqError(nextStateOnOldTraj.state, st);

                if (bestMatchError + trajectorySnappingThreshold < oldTrajError) {
                    currentTrajectory = bestMatch.parentRun;
                    currentTrajectoryStateMatch = bestMatch;
                } else {
                    currentTrajectoryStateMatch = nextStateOnOldTraj;
                    chosenKeys = currentTrajectoryStateMatch.keys;
                }
            } else {
                currentTrajectory = bestMatch.parentRun;
                currentTrajectoryStateMatch = bestMatch;
            }
        } else {
            currentTrajectory = bestMatch.parentRun;
            currentTrajectoryStateMatch = bestMatch;
        }

        previousStatesLIFO.push(st); // Keep previous states too.

        Action currentAction = new Action(1, chosenKeys);

        currentDecision = new DecisionHolder(currentAction, currentTrajectory,
                currentTrajectory.states.indexOf(currentTrajectoryStateMatch));
        return currentAction;
    }

    @Override
    public Action policy(NodeQWOPExplorableBase<?> state, IGameSerializable game) {
        return policy(state);
    }

    @Override
    public IController getCopy() {
        throw new RuntimeException("Haven't implemented copy on this controller yet!");
    }


    private float totalEvalFunction(StateHolder sh, IState actualState) {
        float cost = 0f;

        // Error relative to current state.
        float currentStateError = sqError(sh.state, actualState);
        cost += currentStateError;
        //System.out.print("state error: " + currentStateError);
        int parentRunLength = sh.parentRun.states.size();
        int stateLocInSequence = sh.parentRun.states.indexOf(sh);

        // Sequences near their ends get a penalty.
        if (penalizeEndOfSequences) {
            float futureSequenceSizeError =
                    maxPenaltyForEndOfSequence / (float) (parentRunLength - stateLocInSequence + 1);
            cost += futureSequenceSizeError;
        }
        if (penalizeSlow) {

            float slowError =
                    -penalizeSlowMult * (sh.parentRun.states.get(Integer.min(stateLocInSequence + penalizeSlowHorizon
                            , sh.parentRun.states.size() - 1)).state.body.getX() - sh.parentRun.states.get(stateLocInSequence).state.body.getX()) / penalizeSlowHorizon;
//			float accumulatedVel = 0;
//			for (int i = stateLocInSequence; i < Integer.min(sh.parentRun.states.size(), stateLocInSequence +
// penalizeSlowHorizon); i++) {
//				accumulatedVel += sh.parentRun.states.get(i).state.body.dx;
//			}
//			float slowError = penalizeSlowMult/Float.max(0.001f, accumulatedVel);
            cost += slowError;
        }

        // Also compare previous states.
        if (comparePreviousStates) {
            int count = 1;
            Iterator<IState> iter = previousStatesLIFO.iterator();
            float oldStateError = 0;
            while (iter.hasNext()) {
                IState oldState = iter.next();
                int idx = stateLocInSequence - count;
                if (idx >= 0) {
                    State stateFromLibrary = sh.parentRun.states.get(idx).state;
                    oldStateError += (previousStatePenaltyMult / (float) count) * sqError(oldState, stateFromLibrary);
                } else {
                    oldStateError += previousStatePenaltyMult * currentStateError * 0.5f;
                }
                count++;
            }
            cost += oldStateError;
        }
        return cost;
    }

    /**
     * Sum of squared distance of all values in two states.
     **/
    private float sqError(IState s1, IState s2) {
        float errorAccumulator = 0;
        float xOffset1 = s1.getCenterX();
        float xOffset2 = s2.getCenterX();

        for (ObjectName bodyPart : ObjectName.values()) {
            for (StateName stateVar : StateName.values()) {

                float thisVal = s1.getStateVariableFromName(bodyPart).getStateByName(stateVar) - ((stateVar == StateName.X) ?
                        xOffset1 : 0);
                float otherVal = s2.getStateVariableFromName(bodyPart).getStateByName(stateVar) - ((stateVar == StateName.X) ?
                        xOffset2 : 0);
                float diff = thisVal - otherVal;
                errorAccumulator += weights.getStateVariableFromName(bodyPart).getStateByName(stateVar) * diff * diff;
            }
        }
        return errorAccumulator;
    }

    /**
     * Return current nearest trajectory (if one is chosen).
     **/
    public DecisionHolder getCurrentDecision() {
        return currentDecision;
    }

    /**
     * Handle loading the TFRecords and making the appropriate data structures. Loads them into RunHolder objects,
     * which hold StateHolder objects.
     *
     * @param files A list of *.TFRecord files containing the trajectories we want to use.
     */
    private void loadAll(List<File> files) throws IOException {
        Utility.tic();

        for (File singleSaveFile : files) { // Each file may contain many runs.

            // Read from binary TFRecord to SequenceExample objects.
            List<SequenceExample> dataSeries = TFRecordDataParsers.loadSequencesFromTFRecord(singleSaveFile);

            logger.info("Read " + dataSeries.size() + " runs from file " + singleSaveFile.getName());

            // Process into the more convenient RunHolder format.
            for (SequenceExample singleSequence : dataSeries) { // Each SequenceExample represents 1 run.

                int totalTimestepsInRun = singleSequence.getFeatureLists().getFeatureListMap().get("BODY").getFeatureCount();

                RunHolder rh = new RunHolder();

                // Unpack the game.action as durations. TODO: turn them into full-fledged Actions
                for (int i = 0; i < singleSequence.getFeatureLists().getFeatureListMap().get("ACTIONS").getFeatureCount(); i++) {
                    rh.actionDurations.add(Byte.toUnsignedInt(singleSequence.getFeatureLists().getFeatureListMap()
                            .get("ACTIONS").getFeature(i) // This is the action number
                            .getBytesList().getValue(0) // This only has one element lol
                            .byteAt(0))); // this is the [duration, q,w,o,p]
                }

                //TODO figure out why this was necessary.
                int startPt = killFirstTwoActions ? rh.actionDurations.get(0) + rh.actionDurations.get(1) : 0;

                // Unpack all states.
                for (int i = startPt; i < totalTimestepsInRun; i++) { // Each run has many timesteps.

                    // Unpack each x y th... value in a given timestep. Turn them into StateVariables.
                    Map<String, FeatureList> featureListMap = singleSequence.getFeatureLists().getFeatureListMap();
                    StateVariable[] sVarBuffer = new StateVariable[ObjectName.values().length];

                    int idx = 0;
                    for (ObjectName bodyPart : ObjectName.values()) {
                        List<Float> sValList =
                                featureListMap.get(bodyPart.toString()).getFeature(i).getFloatList().getValueList();

                        sVarBuffer[idx] = new StateVariable(sValList);
                        idx++;
                    }

                    // Turn the StateVariables into a single State for this timestep.
                    State st = new State(sVarBuffer[0], sVarBuffer[1], sVarBuffer[2], sVarBuffer[3], sVarBuffer[4],
                            sVarBuffer[5], sVarBuffer[6], sVarBuffer[7], sVarBuffer[8], sVarBuffer[9], sVarBuffer[10]
                            , sVarBuffer[11], false);

                    // Get game.action as keypresses at the current state.
                    byte[] keyPressBytes =
                            singleSequence.getFeatureLists().getFeatureListMap().get("PRESSED_KEYS").getFeature(i).getBytesList().getValue(0).toByteArray();
                    boolean[] keyPress = new boolean[4];
                    keyPress[0] = keyPressBytes[0] == (byte) 1;
                    keyPress[1] = keyPressBytes[1] == (byte) 1;
                    keyPress[2] = keyPressBytes[2] == (byte) 1;
                    keyPress[3] = keyPressBytes[3] == (byte) 1;

                    StateHolder newState = new StateHolder(st, keyPress, rh);
                    allStates.put(st.getStateVariableFromName(sortByPart).getStateByName(sortBySt), newState);
                    numStatesLoaded++;
                }

                runs.add(rh);
            }
        }
        Utility.toc();
    }

    @Override
    public void close() {}

    /**
     * @author matt
     */
    public static class StateHolder implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * Actual physical state variables.
         **/
        public final State state;

        /**
         * QWOP keys pressed.
         **/
        final boolean[] keys;

        /**
         * What run is this state a part of?
         **/
        final RunHolder parentRun;

        private StateHolder(State state, boolean[] keys, RunHolder parentRun) {
            this.state = state;
            this.keys = keys;
            this.parentRun = parentRun;
            parentRun.addState(this); // Each StateHolder adds itself to the run it's part of. Probably terrible
            // programming, but I'm keeping it pretty contained here.
        }
    }

    /**
     * @author matt
     */
    public static class RunHolder implements Serializable {

        private static final long serialVersionUID = 1L;

        public List<Integer> actionDurations = new ArrayList<>();

        /**
         * All the states seen in this single run.
         **/
        public List<StateHolder> states = new ArrayList<>();

        /**
         * Adds a state, in the order it's seen, to this run. Should only be automatically called.
         **/
        private void addState(StateHolder sh) {
            states.add(sh);
        }
    }

    public static class DecisionHolder implements Serializable {

        private static final long serialVersionUID = 8L;

        public Action chosenAction;

        //public RunHolder chosenTrajectory;

        int chosenIdx;

        DecisionHolder(Action chosenAction, RunHolder chosenTrajectory, int chosenIdx) {
            this.chosenAction = chosenAction;
            this.chosenIdx = chosenIdx / 20;

//			// Thin them down a little.
//			chosenTrajectory = new RunHolder();
//			for (int i = 0; i < chosenTrajectory.states.size(); i += 20) {
//				chosenTrajectory.addState(chosenTrajectory.states.get(i));
//				break;
//			}
        }

    }

    @Override
    public void draw(Graphics g, GameUnified game, float runnerScaling, int xOffsetPixels, int yOffsetPixels) {
        if (!previousStatesLIFO.isEmpty()) {
            g.setColor(Color.WHITE);
            g.drawString(String.valueOf(previousStatesLIFO.peek().getCenterX()), 50, 50);
        }

        if (currentTrajectory != null && currentTrajectoryStateMatch != null) {
            RunHolder drawTraj = currentTrajectory;
            StateHolder drawState = currentTrajectoryStateMatch;
            int startIdx = drawTraj.states.indexOf(drawState);
            float bodyX = currentTrajectoryStateMatch.state.body.getX();
            GameUnified.drawExtraRunner((Graphics2D) g, drawState.state, "", runnerScaling, xOffsetPixels - (int) (runnerScaling * bodyX), yOffsetPixels, Color.CYAN, PanelRunner.normalStroke);

            int viewingHorizon = 80;
            for (int i = 0; i < drawTraj.states.size(); i++) {
                if ((i > startIdx && i < startIdx + viewingHorizon)) { //|| i % 10 == 0) {
                    GameUnified.drawExtraRunner((Graphics2D) g, drawTraj.states.get(i).state, "", runnerScaling,
                            xOffsetPixels - (int) (runnerScaling * bodyX), yOffsetPixels,
                            NodeQWOPGraphicsBase.getColorFromScaledValue(i - startIdx + viewingHorizon, 2 * viewingHorizon,
                                    (viewingHorizon - Math.abs(i - startIdx)) / (float) viewingHorizon), PanelRunner.normalStroke);
                } else if (i < startIdx && i > startIdx - viewingHorizon) {
                    GameUnified.drawExtraRunner((Graphics2D) g, drawTraj.states.get(i).state, "", runnerScaling,
                            xOffsetPixels - (int) (runnerScaling * bodyX), yOffsetPixels,
                            NodeQWOPGraphicsBase.getColorFromScaledValue(i - startIdx + viewingHorizon,
                                    2 * viewingHorizon,
                                    (viewingHorizon - Math.abs(i - startIdx)) / (float) viewingHorizon), PanelRunner.normalStroke);
                }
            }
        }
    }
}
