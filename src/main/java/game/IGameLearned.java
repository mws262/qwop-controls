package game;

import java.util.List;

public interface IGameLearned extends IGame {
    void assembleWholeRunForTraining(List<State> states, List<boolean[]> commands);
}
