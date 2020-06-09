close all; clear;

%% Import the data from text files.
file_directory = '../../tmp1/';
[data, tsSorted] = readPerturbationData(file_directory);
numTs = length(tsSorted)

%% Process each perturbation simulation run.
survivalTimesteps = zeros(numTs, 8); % 8 out of 9 should eventually fail. The ninth will have perturbed the simulation with the action it was already going to take.
survivalTimestepsInVar = zeros(numTs, 9);
deviationDiffs = NaN(2000, numTs);
diffcount = 1;
for i = 1:numTs
    runData = data{i}(1:end - 1, :);
    
    % Find out how long after the perturbations each failure occurs. This
    % In the data, NaNs are used to mark state info after that sim fails.
    
    % Subtract the nominal trajectory from each of the perturbed runs. The
    % first index of the perturbation is the first time any of them diverge
    % from nominal.
    [~, beginningIdx] = find((runData(:, 3:end) - runData(:, 2))', 1, 'first');

    if isempty(beginningIdx)
        beginningIdx = NaN;
    else
        runDiff = runData(beginningIdx:end, 3:end) - runData(beginningIdx:end, 2);
        deviationDiffs(1:size(runDiff,1), diffcount) = sum(abs(runDiff), 2) / 8;
        diffcount = diffcount + 1;
    end
    
    % Count NaNs to determine how many have failed at a given timestep.
    nans = isnan(runData(:, 3:end));
    numFailuresSoFar = sum(nans, 2);
    
    firstNanInVar = zeros(1, 9);
    for j = 1:9
        NaNloc = find(nans(:,j), 1, 'first'); 
        if isempty(NaNloc)
            NaNloc = NaN;
        end
        survivalTimestepsInVar(i, j) = NaNloc - beginningIdx;
    end

    
    % Find when the number of failures has just gone up i.e. another
    % simulation has just fallen.
    failureDiffs = numFailuresSoFar(2:end) - numFailuresSoFar(1:end-1);
    endIdxs = find(failureDiffs, 8);
    failureIdxs = zeros(8, 1);
    count = 1;
    for j = 1:length(endIdxs)
        nextIdx = count + failureDiffs(endIdxs(j)) - 1;
        failureIdxs(count:nextIdx) = endIdxs(j) + 1;
        count = count + failureDiffs(endIdxs(j));
    end
    
    survivalTimesteps(i, :) = (failureIdxs - beginningIdx)';
end

devNans = isnan(deviationDiffs);
validNums = size(deviationDiffs, 2) - sum(devNans, 2);
deviationDiffs(isnan(deviationDiffs)) = 0;
sum(deviationDiffs, 2) ./ validNums;

perturbations = {'NONE', 'Q', 'W', 'O', 'P', 'QO', 'QP', 'WO', 'WP'};
for i = 1:9
    survivalOneVar = survivalTimestepsInVar(:, i);
    survivalOneVar(isnan(survivalOneVar)) = [];
    survivalOneVar = survivalOneVar(1:116);
    ax = subplot(3, 3, i);
    ax.YLim = [0, 0.35];
    hold on;
    histogram(survivalOneVar, 15, 'Normalization', 'probability');
        plot([70, 70], ax.YLim, 'LineWidth', 2);
        plot(2*[70, 70], ax.YLim, 'LineWidth', 2);
        plot(3*[70, 70], ax.YLim, 'LineWidth', 2);

    xlabel('timesteps after perturbation until a fall');
    ylabel(['portion of ', perturbations{i}, '-perturbed simulations']);
end

lengths = zeros(numTs, 1);
for i = 1:length(data)
    t = data{i};
    [~, endIdx] = find(isnan(t)', 1, 'first');
    [~, beginningIdx] = find((t(:, 3:end) - t(:, 2))', 1, 'first');
    lastIdx = find(sum(isnan(t), 2) > 7, 1, 'first');
    
    lengthsShort(i) = endIdx - beginningIdx;
    lengthsLong(i) = lastIdx - beginningIdx;
end

dat = cellfun(@(c)(sum(abs(c(:, 3:end) - c(:, 2))/8, 2)), data, 'UniformOutput', false)
dat = cellfun(@(c)(c(1:length(dat{1}) - 100)), dat, 'UniformOutput', false);



%histogram(lengthsLong(1:1000), 'BinWidth', 20, 'BinLimits', [0, 600])

sumabsdiff = cell2mat(dat);
nanelements = isnan(sumabsdiff);
sumabsdiff(nanelements) = 0;
res = sum(sumabsdiff, 2) ./ (size(sumabsdiff, 2) - sum(nanelements, 2));


col = lines(numTs);

% Nominal trajectory.
fig = figure;
fig.Position = [100, 100, 800, 500];
ax = axes;
hold on;
nominalPl = plot(NaN, NaN, 'LineWidth', 4);
devStartPl = plot(NaN, NaN, 'g', 'LineWidth', 2);
fail1plot = plot(NaN, NaN, 'r', 'LineWidth', 2);
axis([-0.5, 4, -1, 12]);
avgStep = plot([2.5, 2.5], ax.YLim, 'Color', [162, 36, 229]/255, 'LineWidth', 2);
devPlots = plot(0, zeros(1, 9));

perturbationLabels = {"nominal traj", "deviation start", "first fall", "avg step cycle length", "Q", "W", "O", "P", "QO", "QP", "WO", "WP", "NONE"};
xlabel('Time after perturbation (s)');
ylabel('torso angle (rad)');
legen = legend(perturbationLabels);
legen.Position(2) = 0.58;
legen.Position(1) = 0.77;
fig.Color = [1,1,1];

grid on;
vid_name = 'expslowerhead';
vid_writer = VideoWriter([vid_name, '.avi'], 'Uncompressed AVI');
vid_writer.FrameRate = 8;
open(vid_writer);
for j = 1:length(data)
    startT = tsSorted(j) * 0.04;
    devStartPl.XData = [0, 0];
    devStartPl.YData = ax.YLim;
    firstFailIdx = inf;
    
%         data{j}(lastidxplus1 - 1, 1) - tsSorted(j) * 0.04
        devPlots(1).XData =  data{j}(:,1) - startT;
        devPlots(1).YData = sum(abs(data{j}(:, 3:end) - data{j}(:,2)), 2);

            nominalPl.XData = data{end}(:, 1) - startT;
        nominalPl.YData = data{end}(:, 2);

    writeVideo(vid_writer, getframe(fig));

    pause(0.001);
end
close(vid_writer);
                    eval(['!ffmpeg -i ', vid_name, '.avi ', vid_name, '.mp4']);
                    eval(['!rm ', vid_name, '.avi ']); % Remove the temporary avi file.

