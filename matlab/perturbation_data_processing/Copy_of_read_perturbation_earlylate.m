close all; clear;

% Nominal trajectory.
fig = figure;
fig.Position = [100, 100, 800, 500];
ax = axes;
fig.Color = [1,1,1];
for k = 1:14
file_directory = ['../../tmp', num2str(k), '/'];
[data, tsSorted] = readPerturbationData(file_directory);

tsFuture = 25;
% firstTsEarly = zeros(tsFuture, floor(length(data)/4));
% firstTsLate = zeros(tsFuture, floor(length(data)/4));

fastestFailEarly = 1e8;
fastestFailLate = 1e8;
titles = {'none-QP transition', 'QP-none transition', 'none-WO transition', 'WO-none transition'};
for i = 1:4
count = 1;
for j = i:4:length(data)-10 % 1 is none-qp, 2 is qp - none, 3 is none - wo, 4 is wo - none
    [~, beginningIdxEarly] = find((data{j}(:, 3:end) - data{j}(:, 2))', 1, 'first');
    [~, beginningIdxLate] = find((data{j}(:, 4:end) - data{j}(:, 2))', 1, 'first');
    
    firstTsEarly(:, count) = data{j}(beginningIdxEarly - 2: beginningIdxEarly + tsFuture - 3, 3) - data{j}(beginningIdxEarly - 2: beginningIdxEarly + tsFuture - 3, 2);
    firstTsLate(:, count) = data{j}(beginningIdxEarly - 2 : beginningIdxEarly + tsFuture - 3, 4) - data{j}(beginningIdxEarly - 2: beginningIdxEarly + tsFuture - 3, 2);
    count = count + 1;
end
a = mean(firstTsLate, 2);%, 'omitnan');
b = mean(firstTsEarly, 2);%, 'omitnan');
s1 = std(firstTsLate, 1, 2); 
s2 = std(firstTsEarly, 1, 2); 

colors = lines(5);
c1 = colors(1, :);
c2 = colors(2, :);
xspan = -k:length(a) - 1 - k;

ax = subplot(2,2,i, 'replace');
hold on;
ylim([-0.2, 0.2]);
plot(xspan, a, '.', 'MarkerSize', 15, 'LineWidth', 3, 'Color', c1);
plot(xspan, b, '.', 'MarkerSize', 15, 'LineWidth', 3, 'Color', c2);

lateouter = fill([xspan, fliplr(xspan)], [min(firstTsLate, [], 2); flipud(max(firstTsLate, [], 2))], c1, 'FaceAlpha', 0.08, 'LineStyle', 'none');
lateinner = fill([xspan, fliplr(xspan)], [a - s1/2; flipud(a + s1/2)], c1, 'FaceAlpha', 0.2, 'LineStyle', 'none');

earlyouter = fill([xspan, fliplr(xspan)], [min(firstTsEarly, [], 2); flipud(max(firstTsEarly, [], 2))], c2, 'FaceAlpha', 0.08, 'LineStyle', 'none');
earlyinner = fill([xspan, fliplr(xspan)], [b - s2/2; flipud(b + s2/2)], c2, 'FaceAlpha', 0.2, 'LineStyle', 'none');

xlabel('Timesteps after perturbation');
ylabel('Torso angle deviation (rad)');
title(titles{i});
if i == 2
leg = legend({'Late transition avg', 'Early transition avg', 'Late range', 'Late 1 stdev', 'Early range', 'Early 1 stdev'});
leg.Position = [0.7992    0.8202    0.2037    0.1810];
end

plot([0, 0], ax.YLim);
hold off;

end
pause(2);
end
