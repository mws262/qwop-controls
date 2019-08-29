package game;

import game.state.IState;

public interface IGameStateSource<S extends IState> {

    /**
     * Get the runner's state.
     *
     * @return {@link IState} of the runner at the current timestep.
     */
    S getCurrentState();

    /**
     * Get whether the game is considered to be in a failed state.
     * @return Whether the game is failed (true is failed).
     */
    boolean isFailed();

    /**
     * Get the number of timesteps elapsed since the beginning of this run.
     * @return
     */
    long getTimestepsThisGame();

    int getStateDimension();

}
