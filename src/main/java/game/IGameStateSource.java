package game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import game.state.IState;

public interface IGameStateSource<S extends IState> {

    /**
     * Get the runner's state.
     *
     * @return {@link IState} of the runner at the current timestep.
     */
    @JsonIgnore
    S getCurrentState();

    /**
     * Get whether the game is considered to be in a failed state.
     * @return Whether the game is failed (true is failed).
     */
    @JsonIgnore
    boolean isFailed();

    /**
     * Get the number of timesteps elapsed since the beginning of this run.
     * @return
     */
    @JsonIgnore
    long getTimestepsThisGame();

    @JsonIgnore
    int getStateDimension();

}
