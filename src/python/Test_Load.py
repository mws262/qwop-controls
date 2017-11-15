import densedata_pb2 as dataset
import timeit
#import tensorflow as tf
import numpy as np
import matplotlib as mat
import matplotlib.pyplot as plt
import pylab


dataset = dataset.DataSet()
try:
    f = open("../../denseData_2017-11-06_08-58-03.proto","rb")

    start = timeit.default_timer()
    dataset.ParseFromString(f.read())
    end = timeit.default_timer()
    print("Time to parse binary: " + str(end - start) + " seconds")
    f.close
except IOError:
	print "Import failed"

# for runs in dataset.denseData:
#     print(len(runs.state))
#
# print(dataset.denseData[0].state[0].body.y)

bodyX = np.empty(shape=(len(dataset.denseData[0].state), 1), dtype=np.float32)
bodyY = np.empty(shape=(len(dataset.denseData[0].state), 1), dtype=np.float32)
bodyTh = np.empty(shape=(len(dataset.denseData[0].state), 1), dtype=np.float32)


for idx, s in enumerate(dataset.denseData[0].state):
    print str(idx) + "," + str(s.body.x)

    np.put(bodyX, ind=idx, v=s.body.x)
    np.put(bodyY, ind=idx, v=s.body.y)
    np.put(bodyTh, ind=idx, v=s.body.th)


for i in bodyX:
    print i
plt.plot(np.linspace(1,10,np.size(bodyY,0)),bodyX)
plt.plot(np.linspace(1,10,np.size(bodyY,0)),bodyY)
pylab.show()

print bodyX[0]
#print bodyY[0]

print bodyX[-1]
#print bodyY[-1]

