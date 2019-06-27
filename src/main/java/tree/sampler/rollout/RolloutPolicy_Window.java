package tree.sampler.rollout;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.primitives.Floats;
import game.IGameInternal;
import game.action.Action;
import game.action.ActionQueue;
import tree.node.NodeQWOPExplorableBase;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a meta-rollout policy. It does multiple other rollouts and aggregates the results.
 *
 * For now, this is either a best or worst out of three. Intuition says worst of three should be more robust, but so
 * far, best of three has been better. Perhaps because overinflated scores tend to be explored by UCB and toned down,
 * whereas underestimated scores might never be touched again.
 *
 * @author matt
 */
public class RolloutPolicy_Window implements IRolloutPolicy {

    public final IRolloutPolicy individualRollout;

    private ActionQueue actionQueue = new ActionQueue();
    private final List<Action> actionSequence = new ArrayList<>(); // Reused local list.


    public enum Criteria {
        WORST, BEST, AVERAGE,
    }

    public Criteria selectionCriteria = Criteria.BEST;

    public RolloutPolicy_Window(@JsonProperty("individualRollout") IRolloutPolicy individualRollout) {
        this.individualRollout = individualRollout;
    }

    @Override
    public float rollout(NodeQWOPExplorableBase<?> startNode, IGameInternal game) {

        // Need to do a rollout for the actual node we landed on.
        Action middleAction = startNode.getAction();
        float scoreMid = individualRollout.rollout(startNode, game);

        // Pick valid actions above and below -- TODO generalize this to larger windows also.
        List<Action> windowActions = new ArrayList<>();
        windowActions.add(new Action(middleAction.getTimestepsTotal() + 1, middleAction.peek()));
        if (middleAction.getTimestepsTotal() > 1) {
            windowActions.add(new Action(middleAction.getTimestepsTotal() - 1, middleAction.peek()));
        }

        float[] windowScores = new float[windowActions.size()];
        for (int i = 0; i < windowActions.size(); i++) {
            startNode.getParent().getSequence(actionSequence);
            actionQueue.clearAll();
            actionQueue.addSequence(actionSequence);
            actionQueue.addAction(windowActions.get(i));
            game.makeNewWorld();
            while (!actionQueue.isEmpty()) {
                game.step(actionQueue.pollCommand());
            }

            NodeQWOPExplorableBase<?> windowNode = startNode.getParent().addBackwardsLinkedChild(windowActions.get(i),
                    game.getCurrentState());
            windowScores[i] = individualRollout.rollout(windowNode, game);
        }

        switch(selectionCriteria) {

            case WORST:
                return Float.min(scoreMid, Floats.min(windowScores));
            case BEST:
                return Float.max(scoreMid, Floats.max(windowScores));
            case AVERAGE:
                float sum = 0;
                for (float score : windowScores) {
                    sum += score;
                }
                sum += scoreMid;
                return sum / (float)(windowScores.length + 1);
            default:
                throw new IllegalArgumentException("Unknown selection criteria specified: " + selectionCriteria.name());
        }
    }

    @Override
    public IRolloutPolicy getCopy() {
        return new RolloutPolicy_Window(individualRollout.getCopy());
    }

}
