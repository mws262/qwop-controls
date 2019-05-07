package game;

public interface IGameStateSource {

    /**
     * Get the runner's state.
     *
     * @return {@link State} of the runner at the current timestep.
     */
    State getCurrentState();

    /**
     * Get whether the game is considered to be in a failed state.
     * @return Whether the game is failed (true is failed).
     */
    boolean getFailureStatus();

    /**
     * Get the number of timesteps elapsed since the beginning of this run.
     * @return
     */
    long getTimestepsThisGame();

}
