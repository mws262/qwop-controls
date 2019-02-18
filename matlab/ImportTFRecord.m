% Handle Java imports.
eval('javaaddpath ../target/qwop-controls-1.0.jar'); % Strange issues with trying to directly call these commands. Eval seems to fix it.
eval('javaaddpath ../jbox2d.jar');
eval('import game.State game.StateVariable game.GameSingleThreadWithDraw actions.Action actions.ActionQueue data.TFRecordDataParsers');

dataParser = data.TFRecordDataParsers;
loadedFileData = dataParser.loadSequencesFromTFRecord(java.io.File('../src/main/resources/training_data/denseTF_2018-04-26_15-19-44.TFRecord'));
fprintf('loaded %d runs from file.\n', loadedFileData.size());

loadedStates = [];
loadedActions = [];
for i = 0:loadedFileData.size() - 1
    loadedStates = [dataParser.getStatesFromLoadedSequence(loadedFileData.get(i)); loadedStates];
end

bodyY = zeros(1, length(loadedStates));
bodyTh = zeros(1, length(loadedStates));
count = 1;
for i = 1:length(loadedStates)
    if isempty(loadedStates(i))
        continue;
    end
    
    bodyY(count) = loadedStates(i).body.getY();
    bodyTh(count) = loadedStates(i).body.getTh();
    count = count + 1;
end

plot(bodyY', bodyTh', '.')
