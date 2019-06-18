% Simulate a known sequence of game.actions.

% Handle Java imports.
eval('javaaddpath ../target/qwop-controls-1.0-jar-with-dependencies.jar'); % Strange issues with trying to directly call these commands. Eval seems to fix it.
eval('javaaddpath ../jbox2d.jar');
eval('import game.state.State game.state.StateVariable game.GameSingleThread game.actions.Action game.actions.ActionQueue');

% Key combinations used.
noKey = [false, false, false, false];
wo = [false, true, true, false];
qp = [true, false, false, true];

% Queue up a known sequence of keypresses. Action and ActionQueue are Java
% objects.
actionQueue = game.actions.ActionQueue;
actionQueue.addAction(Action(1, noKey));
actionQueue.addAction(Action(34, wo));
actionQueue.addAction(Action(19, noKey));
actionQueue.addAction(Action(45, qp));

actionQueue.addAction(Action(10, noKey));
actionQueue.addAction(Action(38, wo));
actionQueue.addAction(Action(5, noKey));
actionQueue.addAction(Action(31, qp));

actionQueue.addAction(Action(21, noKey));
actionQueue.addAction(Action(21, wo));
actionQueue.addAction(Action(14, noKey));
actionQueue.addAction(Action(35, qp));

actionQueue.addAction(Action(10, noKey));
actionQueue.addAction(Action(23, wo));
actionQueue.addAction(Action(20, noKey));
actionQueue.addAction(Action(24, qp));

actionQueue.addAction(Action(21, noKey));
actionQueue.addAction(Action(20, wo));
actionQueue.addAction(Action(19, noKey));
actionQueue.addAction(Action(21, qp));

actionQueue.addAction(Action(16, noKey));

game = game.GameSingleThread;
[fig, ax, bodyPatches, headTForm] = DrawNewRunner(game.getDebugVertices);

while (~actionQueue.isEmpty)
    command = actionQueue.pollCommand;
    game.step(command);
    
    RedrawRunner(game.getDebugVertices, bodyPatches, headTForm, ax)
    
    pause(0.038);
end


