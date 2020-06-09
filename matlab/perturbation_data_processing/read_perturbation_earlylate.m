function read_perturbation_earlylate
close all; clear;

% Where the data files are located.
dataDirs = {'../../tmp1/'};

showTsBefore = 5;
showTsAfter = 150;

actionSequenceLength = 4;

%% Setup video recording for when the plot evolves over time.
recordVideo = false;
if recordVideo
    vid_name = 'earlylate';
    vid_writer = VideoWriter([vid_name, '.avi'], 'Uncompressed AVI');
    vid_writer.FrameRate = 0.5;
    open(vid_writer);
end

%% Nominal trajectory.
fig = figure;
fig.Position = [100, 100, 1200, 800];
ax = axes;
fig.Color = [1,1,1];
titles = {'none-QP transition', 'QP-none transition', 'none-WO transition', 'WO-none transition'};

%% The different data sets if we want it to evolve over time.
tsToFailEarly = [];
tsToFailLate = [];
for dirIdx = 1:size(dataDirs)
    [data, tsSorted] = readPerturbationData(dataDirs{dirIdx});
    
    %% For all the data in this set, look at each key transition one at a time.
    % 1 is none-qp, 2 is qp - none, 3 is none - wo, 4 is wo - none
    for sequencePosIdx = 1:actionSequenceLength
        count = 1;
        
%         a = cellfun(@(c)(any(isnan(c(:,3)))), data);
%         b = cellfun(@(c)(find(isnan(c(:, 3)), 1, 'first') - find((c(:, 3) - c(:, 2))', 1, 'first')), data, 'UniformOutput', false);
%         firstEarlyFail = min(cell2mat(b(a)));
%         
%         a = cellfun(@(c)(any(isnan(c(:,4)))), data);
%         b = cellfun(@(c)(find(isnan(c(:, 4)), 1, 'first') - find((c(:, 4) - c(:, 2))', 1, 'first')), data, 'UniformOutput', false);
%         firstLateFail = min(cell2mat(b(a)));
        
        for j = sequencePosIdx:actionSequenceLength:length(data)-11 
            idxDev = findDevIdxs(data{j});
            
            beginningIdxEarly = idxDev(1, 1);
            beginningIdxLate = idxDev(2, 1);
            
            failIdxEarly = idxDev(1, 2);
            failIdxLate = idxDev(2, 2);
            
            tsToFailEarly(end + 1) = failIdxEarly - beginningIdxEarly;
            tsToFailLate(end + 1) = failIdxLate - beginningIdxLate;

            idx = beginningIdxLate - (showTsBefore + 1): beginningIdxLate + (showTsAfter - 1);
            firstTsEarly(:, count) = abs(data{j}(idx, 3) - data{j}(idx, 2));
            
            idx = beginningIdxLate - (showTsBefore + 1): beginningIdxLate + (showTsAfter - 1);
            firstTsLate(:, count) = abs(data{j}(idx, 4) - data{j}(idx, 2));
            count = count + 1;
        end
        
        firstEarlyFail = min(tsToFailEarly);
        firstLateFail = min(tsToFailLate);
        
        deviationDatEarly = firstTsEarly(1:firstEarlyFail + showTsBefore, :);
        deviationDatLate = firstTsLate(1:firstLateFail + showTsBefore + 1, :);

        a = mean(deviationDatLate, 2);%, 'omitnan');
        b = mean(deviationDatEarly, 2);%, 'omitnan');
        s1 = std(deviationDatLate, 1, 2);
        s2 = std(deviationDatEarly, 1, 2);
        
        colors = lines(5);
        c1 = colors(1, :);
        c2 = colors(2, :);
        xspanEarly = -showTsBefore : (size(deviationDatEarly, 1) - showTsBefore - 1);
        xspanLate = -showTsBefore : (size(deviationDatLate, 1) - showTsBefore - 1);
        
        ax = subplot(2,2,sequencePosIdx, 'replace');
        hold on;
        xlim([xspanEarly(1), max(xspanEarly(end), xspanLate(end))]);
        ylim([0, 1.2]);
        
        plot([0, 0], ax.YLim, 'k', 'LineWidth', 2);
        plot([-dirIdx, -dirIdx], ax.YLim, '--', 'Color', colors(2, :), 'LineWidth', 1.5);
        plot([dirIdx, dirIdx], ax.YLim,  '--', 'Color', colors(1, :), 'LineWidth', 1.5);
        
        plot(xspanLate, a, '.', 'MarkerSize', 15, 'LineWidth', 3, 'Color', c1);
        plot(xspanEarly, b, '.', 'MarkerSize', 15, 'LineWidth', 3, 'Color', c2);
        
        lateouter = fill([xspanLate, fliplr(xspanLate)], [min(deviationDatLate, [], 2); flipud(max(deviationDatLate, [], 2))], c1, 'FaceAlpha', 0.08, 'LineStyle', 'none');
        lateinner = fill([xspanLate, fliplr(xspanLate)], [a - s1/2; flipud(a + s1/2)], c1, 'FaceAlpha', 0.2, 'LineStyle', 'none');
        
        earlyouter = fill([xspanEarly, fliplr(xspanEarly)], [min(deviationDatEarly, [], 2); flipud(max(deviationDatEarly, [], 2))], c2, 'FaceAlpha', 0.08, 'LineStyle', 'none');
        earlyinner = fill([xspanEarly, fliplr(xspanEarly)], [b - s2/2; flipud(b + s2/2)], c2, 'FaceAlpha', 0.2, 'LineStyle', 'none');
        
        xlabel('Timesteps around keypress transition');
        ylabel('Torso angle deviation (rad)');
        title(titles{sequencePosIdx});
        
        nonespacing = 13;
        keyspacing = 22;
        if sequencePosIdx == 1 || sequencePosIdx == 3
            plot([keyspacing, keyspacing], ax.YLim, 'k', 'LineWidth', 2);
            plot([nonespacing, nonespacing] + [keyspacing, keyspacing], ax.YLim, 'k', 'LineWidth', 2);
            plot([nonespacing, nonespacing] + [keyspacing, keyspacing] * 2, ax.YLim, 'k', 'LineWidth', 2);
            plot([nonespacing, nonespacing] * 2 + [keyspacing, keyspacing] * 2, ax.YLim, 'k', 'LineWidth', 2);
        else
            plot([nonespacing, nonespacing], ax.YLim, 'k', 'LineWidth', 2);
            plot([nonespacing, nonespacing] + [keyspacing, keyspacing], ax.YLim, 'k', 'LineWidth', 2);
            plot([nonespacing, nonespacing] * 2 + [keyspacing, keyspacing], ax.YLim, 'k', 'LineWidth', 2);
        end
        
        if sequencePosIdx == 4
            leg = legend({'Nominal transition timestep', 'Early transition', 'Late transition', 'Late transition avg', 'Early transition avg', 'Late range', 'Late 1 stdev', 'Early range', 'Early 1 stdev'});
            leg.Position = [0.42    0.01    0.2037    0.1810];
        end
            
        grid on;
        hold off;   
    end
    
    if recordVideo
        writeVideo(vid_writer, getframe(fig)); %#ok<*UNRCH>
    end
end

if recordVideo
    close(vid_writer);
    eval(['!ffmpeg -i ', vid_name, '.avi ', vid_name, '.mp4']);
    eval(['!rm ', vid_name, '.avi ']); % Remove the temporary avi file.
end

    function devIdxs = findDevIdxs(data)
        % Assume 1st column is time, second column is baseline, all
        % additional columns are data which might eventually deviate from
        % baseline.
        % First returned column is where the states begin to deviate.
        % Second returned column is when they turn NaN, i.e. fail.
        
        baselineCol = 2;
        devIdxs = zeros(size(data, 2) - baselineCol, 2);
        
        for m = 1:size(devIdxs, 1)
            [~, idxSt] = find((data(:, m + baselineCol) - data(:, baselineCol))', 1, 'first');
            if isempty(idxSt) % Insert an NaN rather than trying to put in an empty matrix.
                idxSt = NaN;
            end
            devIdxs(m, 1) = idxSt;
            
            [~, idxEnd] = find(isnan(data(:, m + baselineCol))', 1, 'first');
            if isempty(idxEnd) % Insert an NaN rather than trying to put in an empty matrix.
                idxEnd = NaN;
            end
            devIdxs(m, 2) = idxEnd;
        end
    end
end
