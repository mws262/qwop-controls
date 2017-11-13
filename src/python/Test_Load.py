import densedata_pb2
import sys


dataset = densedata_pb2.DataSet()
try:
	f = open("../../denseData_2017-11-06_08-53-50.proto","rb")
	dataset.ParseFromString(f.read())
	f.close
except IOError:
	print "BAD"

print(dataset.denseData[0].state[0].body.y)