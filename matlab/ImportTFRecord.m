% Handle Java imports.
clear java;
% Run maven's package phase if the jar isn't found.
eval('javaaddpath ../target/qwop-controls-1.0-jar-with-dependencies.jar'); % Strange issues with trying to directly call these commands. Eval seems to fix it.
eval('javaaddpath ../jbox2d.jar');
eval('import game.qwop.StateQWOP game.state.StateVariable6D game.qwop.GameQWOP game.action.Action game.action.ActionQueue data.TFRecordDataParsers');

files = dir('../src/main/resources/saved_data/training_data/');
dataParser = data.TFRecordDataParsers;

loadedStates = [];
loadedCommands = [];
for k = 1:length(files)
    fileSplit = strsplit(files(k).name, '.');
    if (files(k).isdir || ~strcmp(fileSplit{2}, 'TFRecord'))
        continue;
    end
    loadedFileData = dataParser.loadSequencesFromTFRecord(java.io.File(strcat(files(k).folder, '/', files(k).name)));
    fprintf('loaded %d runs from file.\n', loadedFileData.size());
    
    for i = 0:loadedFileData.size() - 1
        loadedStates = dataParser.getStatesFromLoadedSequence(loadedFileData.get(i));
        loadedCommands = dataParser.getCommandSequenceFromLoadedSequence(loadedFileData.get(i));
        
        bodyY = zeros(1, length(loadedStates));
        bodyTh = zeros(1, length(loadedStates));
        count = 1;
        for j = 1:length(loadedStates)
            %     if isempty(loadedStates(i))
            %         continue;
            %     end
            bodyY(count) = loadedStates(j).body.getY();
            bodyTh(count) = loadedStates(j).body.getTh();
            count = count + 1;
        end
        noneIdx = loadedCommands(:, 1) == 0 & loadedCommands(:, 2) == 0;
        woIdx = loadedCommands(:, 2) == 1;
        qpIdx = loadedCommands(:, 1) == 1;
        subplot(3,1,1);
        axis([-5,5,-5,5]);
        plot(bodyY(:, noneIdx)', bodyTh(:, noneIdx)', '.r');
        hold on;
        subplot(3,1,2);
        axis([-5,5,-5,5]);
        
        plot(bodyY(:, woIdx)', bodyTh(:, woIdx)', '.g');
        hold on;
        subplot(3,1,3);
        axis([-5,5,-5,5]);
        
        plot(bodyY(:, qpIdx)', bodyTh(:, qpIdx)', '.b');
        hold on;
        drawnow;
    end
    
end

% bodyY = zeros(1, length(loadedStates));
% bodyTh = zeros(1, length(loadedStates));
% count = 1;
% for i = 1:length(loadedStates)
%     %     if isempty(loadedStates(i))
%     %         continue;
%     %     end
%     bodyY(count) = loadedStates(i).body.getY();
%     bodyTh(count) = loadedStates(i).body.getTh();
%     count = count + 1;
% end
%
% noneIdx = loadedCommands(:, 1) == 0 & loadedCommands(:, 2) == 0;
% woIdx = loadedCommands(:, 2) == 1;
% qpIdx = loadedCommands(:, 1) == 1;
% plot(bodyY(:, noneIdx)', bodyTh(:, noneIdx)', '.r');
% hold on;
% plot(bodyY(:, woIdx)', bodyTh(:, woIdx)', '.g');
% plot(bodyY(:, qpIdx)', bodyTh(:, qpIdx)', '.b');


% game = game.GameSingleThread;
% [fig, ax, bodyPatches, headTForm] = DrawNewRunner(game.getDebugVertices);
%
% for i = 1:size(loadedCommands, 1)
%     game.step(loadedCommands(i,:));
%
%     RedrawRunner(game.getDebugVertices, bodyPatches, headTForm, ax)
%
%     pause(0.001);
% end

