function RedrawRunner(vertexHolder, bodyPatches, headTForm, ax)
ax.CameraPosition(1) = vertexHolder.torsoX;
ax.CameraTarget(1) = vertexHolder.torsoX;
ax.XLim = vertexHolder.torsoX + [-10, 10];
for i = 1:size(vertexHolder.bodyVerts,1)
    bodyPatches(i).XData = vertexHolder.bodyVerts(i,1:2:end);
    bodyPatches(i).YData = -vertexHolder.bodyVerts(i,2:2:end);
end
headTForm.Matrix(1,4) = vertexHolder.headLocAndRadius(1);
headTForm.Matrix(2,4) = -vertexHolder.headLocAndRadius(2);
end

