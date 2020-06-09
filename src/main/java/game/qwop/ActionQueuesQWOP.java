package game.qwop;

import game.action.Action;
import game.action.ActionQueue;

public final class ActionQueuesQWOP {

    private ActionQueuesQWOP() {}

    /**
     * Get some sample game.command for use in tests. This is a successful short run found by tree search.
     * @return A successful queue of game.command.
     */
    public static ActionQueue<CommandQWOP> makeShortQueue() {
        int[] durations = new int[]{
                27, 12, 10, 44,
                16, 18, 23, 12,
                21, 12, 17, 13,
                13, 10, 16, 8,
                24, 10, 23, 10,
                24, 34, 5, 18,
                20, 12, 3, 8,
                11, 9, 3, 9
        };

        return makeRepeatingSeriesFour(durations);
    }

    public static ActionQueue<CommandQWOP> makeLongQueue() {
        int[] durations = new int[]{
                7, 49, 2, 25,
                34, 20, 14, 21,
                11, 23, 14, 22,
                12, 24, 13, 21,
                10, 20, 11, 21,
                11, 20, 14, 23,

                13, 22, 10, 22,
                12, 23, 11, 24,
                13, 20, 11, 22,
                14, 20, 10, 21,
                13, 21, 10, 21,
                13, 20, 10, 24,
                10, 23, 10, 22,

                11, 21, 13, 22,
                13, 20, 10, 21,
                13, 22, 14, 22,
                14, 21, 13, 23,
                13, 22, 13, 22,
                12, 21, 14, 21,
                11, 22, 10, 21,

                13, 21, 14, 21,
                13, 21, 11, 22,
                11, 23, 11, 22,
                14, 22, 13, 20,
                13, 24, 10, 22,
                13, 23, 12, 22,
                14, 23, 10, 22,

                14, 21, 10, 23,
                12, 22, 14, 20,
                10, 20
        };
        return makeRepeatingSeriesFour(durations);
    }

    private static ActionQueue<CommandQWOP> makeRepeatingSeriesFour(int[] durations) {
        ActionQueue<CommandQWOP> actionQueue = new ActionQueue<>();

        for (int i = 0; i < durations.length - 4; i += 4) {
            actionQueue.addAction(new Action<>(durations[i], CommandQWOP.NONE));
            actionQueue.addAction(new Action<>(durations[i + 1], CommandQWOP.WO));
            actionQueue.addAction(new Action<>(durations[i + 2], CommandQWOP.NONE));
            actionQueue.addAction(new Action<>(durations[i + 3], CommandQWOP.QP));
        }
        int addedIdx = (durations.length / 4) * 4;
        int remainder = durations.length - addedIdx;

        if (remainder > 0)
            actionQueue.addAction(new Action<>(durations[addedIdx], CommandQWOP.NONE));
        if (remainder > 1)
            actionQueue.addAction(new Action<>(durations[addedIdx + 1], CommandQWOP.WO));
        if (remainder > 2)
            actionQueue.addAction(new Action<>(durations[addedIdx + 2], CommandQWOP.QP));

        return actionQueue;
    }
}
