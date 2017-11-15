import densedata_pb2
import sys
import timeit



dataset = densedata_pb2.DataSet()
try:
	start = timeit.timeit();
	f = open("../../denseData_2017-11-06_08-53-50.proto","rb")
	end = timeit.timeit()
	print("Time to open binary: " + str(end - start) + " seconds")

	start = timeit.timeit();
	dataset.ParseFromString(f.read())
	end = timeit.timeit()
	print("Time to parse binary: " + str(end - start) + " seconds")
	f.close
except IOError:
	print "Import failed"

for runs in dataset.denseData:
	print(len(runs.state))

print(dataset.denseData[0].state[0].body.y)