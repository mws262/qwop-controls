close all;

reader = VideoReader('record_run.mp4');


fig = figure; hold on;
fig.Color = [1,1,1,0];
dotAxes = axes;
dotAxes.Visible = 'off';

qpl = plot(3, 14,'b.'); hold on;
wpl = plot(42, 14, 'b.');
opl = plot(450, 14, 'b.');
ppl = plot(495, 14, 'b.');
axis([0, 500, 0, 28]);

small = 20;
big = 40;

figure;
currAxes = axes;
numFrames = ceil(reader.Duration * reader.FrameRate);

keys = zeros(numFrames, 4);
currIdx = 1;

toriginal = 0 : 1/reader.FrameRate : reader.Duration;
tinterp = 0 : 1 / 30 : reader.Duration;
while reader.hasFrame()
       vidFrame = readFrame(reader);
       qBlue = vidFrame(14, 3, 3);
       wBlue = vidFrame(14, 42, 3);
       oBlue = vidFrame(14, 450, 3);
       pBlue = vidFrame(14, 495, 3);
       qDown = qBlue < 150;
       wDown = wBlue < 150;
       oDown = oBlue < 150;
       pDown = pBlue < 150;
       
       keys(currIdx, :) = [qDown, wDown, oDown, pDown];
       currIdx = currIdx + 1;
       
%        image(vidFrame, 'Parent', currAxes);
%        if qDown
%            qpl.MarkerSize = small;
%        else
%            qpl.MarkerSize = big;
%        end
%        
%        if wDown
%            wpl.MarkerSize = small;
%        else
%            wpl.MarkerSize = big;
%        end
%        
%        if oDown
%            opl.MarkerSize = small;
%        else
%            opl.MarkerSize = big;
%        end
% 
%        if pDown
%            ppl.MarkerSize = small;
%        else
%            ppl.MarkerSize = big;
%        end
% 
%             currAxes.Visible = 'off';
%             drawnow;
%              pause(1/reader.FrameRate);
   
end

keyInterpRaw = interp1(toriginal, keys, tinterp);
keyInterpProcess = round(keyInterpRaw(1:end-1, :));

for i = 1:size(keyInterpProcess)
   fprintf('{%i, %i, %i, %i},\n', ...
   keyInterpProcess(i, 1), ...
   keyInterpProcess(i, 2), ...
   keyInterpProcess(i, 3), ...
   keyInterpProcess(i, 4));
    
end

