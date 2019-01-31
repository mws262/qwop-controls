package controllers;

import actions.Action;
import game.State;
import org.deeplearning4j.rl4j.policy.DQNPolicy;
import org.deeplearning4j.rl4j.space.Box;
import org.nd4j.linalg.cpu.nativecpu.NDArray;

import java.io.IOException;

public class Controller_RL4J implements IController {

    private DQNPolicy<Box> policy;

    public Controller_RL4J(String policySavePath) {
        try {
            policy = DQNPolicy.load(policySavePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Action policy(State state) {
        Integer actionIndex = policy.nextAction(new NDArray(state.flattenState()));
        switch(actionIndex) {
            case 0:
                return new Action(1, false, false, false, false);
            case 1:
                return new Action(1, false, true, true, false);
            case 2:
                return new Action(1, true, false, false, true);
            default:
                throw new IllegalArgumentException("Bad action index: " + actionIndex);
        }
    }


}
