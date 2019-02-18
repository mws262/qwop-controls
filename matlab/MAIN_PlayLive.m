function MAIN_PlayLive
% Set up a "playable" version of the game in MATLAB using the Java
% implementation of QWOP. Q, W, O, P keys work as usual when the window is
% in focus. R resets the game. NOTE: MATLAB key listening is patchy when
% multiple keys are pressed in short succession. Hence the behavior of this
% playable game may not be what you expect.

% Handle Java imports.
eval('javaaddpath ../target/qwop-controls-1.0.jar'); % Strange issues with trying to directly call these commands. Eval seems to fix it.
eval('javaaddpath ../jbox2d.jar');
eval('import game.State game.StateVariable game.GameSingleThread');

% Make a new game (implementation in Java).
qwopGame = game.GameSingleThread;

% Create graphics objects.
close all;
[fig, ax, bodyLinks, headTForm] = DrawNewRunner(qwopGame.getDebugVertices);

% Add keyboard listening for QWOP keys.
q = false; % Keep track of which keys are currently down.
w = false;
o = false;
p = false;

fig.KeyPressFcn = @keyPress;
fig.KeyReleaseFcn = @keyRelease;

% Loop game until 
while ishandle(fig)
    qwopGame.step(q,w,o,p)
    RedrawRunner(qwopGame.getDebugVertices, bodyLinks, headTForm, ax);
    drawnow();
    pause(0.03);
end

    function keyPress(src, data)
        switch data.Key
            case 'q'
                q = true;
            case 'w'
                w = true;
            case 'o'
                o = true;
            case 'p'
                p = true;
            case 'r' % Reset the game.
                qwopGame.makeNewWorld;
                q = false;
                w = false;
                o = false;
                p = false;
        end  
    end

    function keyRelease(src, data)
        switch data.Key
            case 'q'
                q = false;
            case 'w'
                w = false;
            case 'o'
                o = false;
            case 'p'
                p = false;
        end 
    end
end
