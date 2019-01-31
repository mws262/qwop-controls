package goals.deep_rl;

import game.GameThreadSafe;
import game.State;
import org.deeplearning4j.gym.StepReply;
import org.deeplearning4j.rl4j.mdp.MDP;
import org.deeplearning4j.rl4j.space.ArrayObservationSpace;
import org.deeplearning4j.rl4j.space.DiscreteSpace;
import org.deeplearning4j.rl4j.space.ObservationSpace;
import org.jblas.util.Random;
import org.json.JSONObject;

import java.awt.*;

public class QWOP_MDP_Threadable implements MDP<QWOPStateRL, Integer, DiscreteSpace> {

    private ObservationSpace<QWOPStateRL> observationSpace = new ArrayObservationSpace(new int[] {72});
    private DiscreteSpace actionSpace = new DiscreteSpace(3);

    private GameThreadSafe game = new GameThreadSafe();

    @Override
    public ObservationSpace<QWOPStateRL> getObservationSpace() {
        return observationSpace;
    }

    @Override
    public DiscreteSpace getActionSpace() {
        return actionSpace;
    }

    @Override
    public QWOPStateRL reset() {
        State lastState = game.getCurrentState();
        System.out.println(lastState.body.getX() + "," + game.getTimestepsSimulatedThisGame());

        game.makeNewWorld();
        return new QWOPStateRL(game.getCurrentState());
    }

    @Override
    public void close() {

    }

    @SuppressWarnings("Duplicates")
    @Override
    public StepReply<QWOPStateRL> step(Integer actionIndex) {

        switch(actionIndex) {
            case 0:
                game.stepGame(false, false, false, false);
                break;
            case 1:
                game.stepGame(false, true, true, false);
                break;
            case 2:
                game.stepGame(true, false, false, true);
                break;
            default:
                throw new IllegalArgumentException("Bad action index: " + actionIndex);
        }
        State currentState = game.getCurrentState();
        //game.applyBodyImpulse(0.01f*(Random.nextFloat() - 0.5f), 0.01f*(Random.nextFloat() - 0.5f));

        return new StepReply<>(new QWOPStateRL(currentState),
                currentState.body.getX() - 0.1f*currentState.body.getY() + 0.1f*currentState.body.getDx(),
                isDone(),
                new JSONObject("{}"));
    }

    @Override
    public boolean isDone() {
        return game.getFailureStatus();
    }

    @Override
    public MDP<QWOPStateRL, Integer, DiscreteSpace> newInstance() {
        return new QWOP_MDP_Threadable();
    }
}
