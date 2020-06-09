function [data, timesteps] = readPerturbationData(file_directory)
files = dir([file_directory, '*.txt']);

num_files = length(files);
data = cell(1, num_files);
fileOrder = zeros(1, num_files);
for i = 1:length(files)
    fileOrder(i) = str2double(regexp(files(i).name,'\d*','Match')); % Extract the number portion from the file name.
    data{i} = importdata([file_directory, files(i).name]); % Load the actual data from the file.
end
% Reorder s.t. the order in the cell array matches the numbered ordering
% of the files.
[timesteps, idx] = sort(fileOrder);
data = {data{idx}};
end

