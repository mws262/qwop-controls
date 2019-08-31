package game.qwop;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import game.IGameSerializable;

import java.awt.*;
import java.util.Arrays;
import java.util.LinkedList;

public class GameQWOPCaching<S extends StateQWOPDelayEmbedded> implements IGameSerializable<CommandQWOP, S> {

    @JsonIgnore
    public final int STATE_SIZE;

    public enum StateType {
        POSES, DIFFERENCES, HIGHER_DIFFERENCES
    }
    // Caches normal states to then assemble the delay-embedded ones.
    private LinkedList<StateQWOP> cachedStates;

    // For delay embedding.
    @JsonProperty("timestepDelay")
    public final int timestepDelay;
    @JsonProperty("numDelayedStates")
    public final int numDelayedStates;
    @JsonProperty("stateType")
    public final StateType stateType;

    private GameQWOP game;

    public GameQWOPCaching(@JsonProperty("timestepDelay") int timestepDelay,
                           @JsonProperty("numDelayedStates") int numDelayedStates,
                           @JsonProperty("stateType") StateType stateType) {
        game = new GameQWOP();
        resetGame();
        if (timestepDelay < 1) {
            throw new IllegalArgumentException("Timestep delay must be at least one. Was: " + timestepDelay);
        }
        this.timestepDelay = timestepDelay;
        this.numDelayedStates = numDelayedStates;

        STATE_SIZE = (numDelayedStates + 1) * 36;
        this.stateType = stateType;
    }

    // Only for internal use when restoring serialized states.
    private GameQWOPCaching(int timestepDelay, int numDelayedStates, StateType stateType, GameQWOP restoredGame) {
        this(timestepDelay, numDelayedStates, stateType);
        this.game = restoredGame;
    }

    public void setPhysicsIterations(int iterations) {
        game.setPhysicsIterations(iterations);
    }

    @Override
    public void resetGame() {
        game.resetGame();

        if (cachedStates == null) {
            cachedStates = new LinkedList<>();
        }
        cachedStates.clear();
        cachedStates.add(GameQWOP.getInitialState());
    }

    @Override
    public void step(CommandQWOP c) {
        game.step(c);
        cachedStates.addFirst(game.getCurrentState());
    }

    @Override
    public void command(CommandQWOP command) {
        step(command);
    }

    @Override
    public int getNumberOfChoices() {
        return game.getNumberOfChoices();
    }

    @Override
    public void draw(Graphics g, float runnerScaling, int xOffsetPixels, int yOffsetPixels) {
        game.draw(g, runnerScaling, xOffsetPixels, yOffsetPixels);
    }

    @Override
    public S getCurrentState() {

        StateQWOP[] states = new StateQWOP[numDelayedStates + 1];
        Arrays.fill(states, GameQWOP.getInitialState());

        for (int i = 0; i < Integer.min(states.length, (cachedStates.size() + timestepDelay - 1) / timestepDelay); i++) {
            states[i] = cachedStates.get(timestepDelay * i);
        }

        // TODO fix this gross below
        switch (stateType) {
            case POSES:
                return (S) new StateQWOPDelayEmbedded_Poses(states);
            case DIFFERENCES:
                return (S) new StateQWOPDelayEmbedded_Differences(states);
            case HIGHER_DIFFERENCES:
                return (S) new StateQWOPDelayEmbedded_HigherDifferences(states);
            default:
                throw new IllegalStateException("Unhandled state return type.");
        }
    }

    @Override
    public boolean isFailed() {
        return game.isFailed();
    }

    @Override
    public long getTimestepsThisGame() {
        return game.getTimestepsThisGame();
    }

    @Override
    public void setState(S state) {
        cachedStates.clear();
        for (int i = state.individualStates.length - 1; i >= 0; i--) { // element zero is the newest.
            cachedStates.add(state.individualStates[i]);
        }
        game.setState(state.individualStates[0]);
    }

    @Override
    public int getStateDimension() {
        return STATE_SIZE;
    }

    @Override
    public byte[] getSerializedState() {
        return game.getSerializedState();
    }

    @Override
    public IGameSerializable<CommandQWOP, S> restoreSerializedState(byte[] fullState) {
        return new GameQWOPCaching<>(timestepDelay, numDelayedStates, stateType,
                game.restoreSerializedState(fullState));
    }

    @Override
    public GameQWOPCaching<S> getCopy() {
        return new GameQWOPCaching<>(timestepDelay, numDelayedStates, stateType);
    }
}
