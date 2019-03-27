package savers;

import actions.Action;
import game.GameColdStartCorrected;
import game.GameLearned;
import game.GameLearnedSingle;
import tree.Node;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Instead of actually saving to file, this uses the same pipeline to send the data to some network which is being
 * trained.
 */
public class DataSaver_SendToTraining extends DataSaver_Dense {

    GameColdStartCorrected gameLearned;

    public DataSaver_SendToTraining(GameColdStartCorrected gameLearned) {
        this.gameLearned = gameLearned;
    }
    @Override
    public void reportGameEnding(Node endNode) {
        if (stateBuffer.size() > 3) {
            List<boolean[]> actionBooleans = actionBuffer.stream().map(Action::peek).collect(Collectors.toList());
            gameLearned.doTrainingOnRun(stateBuffer, actionBooleans);
            gameLearned.saveCheckpoint("simchk");
        }

        stateBuffer.clear();
        actionBuffer.clear();
    }

    @Override
    public void finalizeSaverData() {}

    @Override
    public DataSaver_SendToTraining getCopy() {
        return new DataSaver_SendToTraining(gameLearned);
    }
}
