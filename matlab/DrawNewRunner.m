function [fig, ax, bodyPatches, headTForm] = DrawNewRunner(vertexHolder)
import game.State game.StateVariable game.GameSingleThreadWithDraw
if nargin == 0
   game = GameSingleThread;
   vertexHolder = game.getDebugVertices;
end
fig = figure;
ax = axes;
hold on;
runnerColor = [0.4, 0.4, 0.7];
for i = 1:size(vertexHolder.bodyVerts,1)
    bodyPatches(i) = patch(vertexHolder.bodyVerts(i,1:2:end), -vertexHolder.bodyVerts(i,2:2:end), runnerColor);
end

headTForm = circleApproxPatch(vertexHolder.headLocAndRadius(3));
headTForm.Matrix(1,4) = vertexHolder.headLocAndRadius(1);
headTForm.Matrix(2,4) = -vertexHolder.headLocAndRadius(2);
plot([-100, 100], -[vertexHolder.groundHeight, vertexHolder.groundHeight]);

fig.Position = [100,100, 800, 600];
ax.Clipping = 'off';
ax.Color = 'none';
ax.YColor = 'none';

axis equal;
ylim([-10, 8]);

    function [circleTForm, circlePatch] = circleApproxPatch(radius)
        angles = linspace(0, 2*pi, 36);
        x = cos(angles) * radius;
        y = sin(angles) * radius;
        circlePatch = patch(x,y,runnerColor);
        circleTForm = hgtransform;
        circlePatch.Parent = circleTForm;
    end
end
