clear all;
stateData = importdata("../vision_capture/run12/poses.dat");
xvelactual = stateData.data(:,37);

groundpix = [387, 290];
pix2worldscaling = 1.3476;
pixvel = zeros(length(xvelactual - 1),1);
for j = 0:length(xvelactual) - 2
    fr1 = rgb2gray(imread("../vision_capture/run12/ts" + j + ".png"));
    fr2 = rgb2gray(imread("../vision_capture/run12/ts" + (j + 1) + ".png"));
    % imshow(fr1);
    % figure;
    % imshow(fr2);


    row1 = double(fr1(groundpix(1), :));
    row2 = double(fr2(groundpix(1), :));

    windowSize = 50;
    originalPix = row1(groundpix(2):groundpix(2) + windowSize);

    correlations = zeros(length(row1) - windowSize, 1);
    for i=1:length(row1) - windowSize
       correlations(i, 1) = sum(originalPix .* (row2(i:i + windowSize) / norm(row2(i:i + windowSize)))); 
    end
    close all;
    plot(correlations);
    [~, maxpix] = max(correlations);
    pixvel(j + 1) = groundpix(2) - maxpix;
end

% [B,A] = butter(2, 0.5, 'low');
% filtdat = filter(B, A, pix2worldscaling * pixvel);
plot(xvelactual);
hold on;
plot(pix2worldscaling * pixvel);
% plot(filtdat);

fun = @(coeff)(sum((xvelactual(1:end - 10) - coeff .* pixvel(1:end-10, 1)).^2));
fminsearch(fun, 10)