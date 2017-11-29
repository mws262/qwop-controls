import numpy as np
import densedata_pb2 as dataset
import tensorflow as tf
import time
import timeit
import os.path


DISCARD_END_TS_NUM = 100

# Define state order
NUM_BODY_PARTS = 12
NUM_STATES_PER = 6

BODY = 0
RTHIGH = 1
RCALF = 2
RFOOT = 3
LTHIGH = 4
LCALF = 5
LFOOT = 6
RUARM = 7
RLARM = 8
LUARM = 9
LLARM = 10
HEAD = 11

def extract_games(f):
  """Extract the games into a list of 2D numpy arrays [timestep, [x, y, th, dx, dy, dth]].
  Args:
    f: A file object
  Raises:
    ValueError: If the bytestream does not start with 2051.
  """
    #f = open("../../denseData_2017-11-06_08-58-03.proto", "rb")

  dat = dataset.DataSet()
  try:
      start = timeit.default_timer()
      dat.ParseFromString(f.read())
      end = timeit.default_timer()
      print("Time to parse binary: " + str(end - start) + " seconds")
      f.close
  except IOError:
      print "Import failed"

  ## Outer for loop here
  allStatesInFile = []
  allActionssInFile = []
  justTSToTransition = []
  for gameIdx, game in enumerate(dat.denseData):
      num_timesteps = len(game.state)
      cutoffTS = num_timesteps - 1 # Normally cuts off at the last index

      #### ACTIONS ####
      singleGameActions = {'q' : list(), 'w' : list(), 'o' : list(), 'p' : list(), 'ts_hold' : list(), 'ts_to_transition' : list()}
      for tsIdx, a in enumerate(game.action):

          if tsIdx == 0: # For first index, we need to discover when the next transition is. Otherwise, we can just subtract one from the previously found one.
              prevTrans = 0
          else:
              prevTrans = singleGameActions['ts_to_transition'][-1]

          if prevTrans > 1:
                singleGameActions['ts_to_transition'].append(prevTrans - 1)
                justTSToTransition.append(prevTrans - 1)
          else:
              if num_timesteps - tsIdx - a.actionTimesteps > DISCARD_END_TS_NUM:  # If after this next action we'll be below the "ignore the end timesteps count," just stop.
                  tsToTransition = a.actionTimesteps
                  singleGameActions['ts_to_transition'].append(tsToTransition)
                  justTSToTransition.append(prevTrans - 1)
              else:
                  cutoffTS = tsIdx - 1 # Otherwise cuts off at index before this one, and skips this one.
                  break

          singleGameActions['q'].append(a.Q)
          singleGameActions['w'].append(a.W)
          singleGameActions['o'].append(a.O)
          singleGameActions['p'].append(a.P)
          singleGameActions['ts_hold'].append(a.actionTimesteps)


      allActionssInFile.append(singleGameActions)

      print cutoffTS

      #### STATES ####
      singleGame = np.zeros(shape=(cutoffTS + 1, NUM_BODY_PARTS * NUM_STATES_PER), dtype=np.float32)
      for tsIdx, s in enumerate(game.state):
          if tsIdx > cutoffTS:
              print tsIdx
              break

          singleIndex = tsIdx * NUM_STATES_PER * NUM_BODY_PARTS
          # TODO stop hardcoding this stuff
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * BODY], v=s.body.x)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * BODY + 1], v=s.body.y)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * BODY + 2], v=s.body.th)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * BODY + 3], v=s.body.dx)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * BODY + 4], v=s.body.dy)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * BODY + 5], v=s.body.dth)

          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * RTHIGH], v=s.rthigh.x)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * RTHIGH + 1], v=s.rthigh.y)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * RTHIGH + 2], v=s.rthigh.th)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * RTHIGH + 3], v=s.rthigh.dx)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * RTHIGH + 4], v=s.rthigh.dy)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * RTHIGH + 5], v=s.rthigh.dth)

          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * RCALF], v=s.rcalf.x)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * RCALF + 1], v=s.rcalf.y)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * RCALF + 2], v=s.rcalf.th)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * RCALF + 3], v=s.rcalf.dx)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * RCALF + 4], v=s.rcalf.dy)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * RCALF + 5], v=s.rcalf.dth)

          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * RFOOT], v=s.rfoot.x)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * RFOOT + 1], v=s.rfoot.y)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * RFOOT + 2], v=s.rfoot.th)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * RFOOT + 3], v=s.rfoot.dx)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * RFOOT + 4], v=s.rfoot.dy)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * RFOOT + 5], v=s.rfoot.dth)


          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * LTHIGH], v=s.lthigh.x)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * LTHIGH + 1], v=s.lthigh.y)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * LTHIGH + 2], v=s.lthigh.th)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * LTHIGH + 3], v=s.lthigh.dx)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * LTHIGH + 4], v=s.lthigh.dy)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * LTHIGH + 5], v=s.lthigh.dth)

          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * LCALF], v=s.lcalf.x)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * LCALF + 1], v=s.lcalf.y)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * LCALF + 2], v=s.lcalf.th)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * LCALF + 3], v=s.lcalf.dx)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * LCALF + 4], v=s.lcalf.dy)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * LCALF + 5], v=s.lcalf.dth)

          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * LFOOT], v=s.lfoot.x)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * LFOOT + 1], v=s.lfoot.y)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * LFOOT + 2], v=s.lfoot.th)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * LFOOT + 3], v=s.lfoot.dx)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * LFOOT + 4], v=s.lfoot.dy)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * LFOOT + 5], v=s.lfoot.dth)


          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * RUARM], v=s.ruarm.x)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * RUARM + 1], v=s.ruarm.y)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * RUARM + 2], v=s.ruarm.th)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * RUARM + 3], v=s.ruarm.dx)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * RUARM + 4], v=s.ruarm.dy)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * RUARM + 5], v=s.ruarm.dth)

          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * RLARM], v=s.rlarm.x)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * RLARM + 1], v=s.rlarm.y)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * RLARM + 2], v=s.rlarm.th)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * RLARM + 3], v=s.rlarm.dx)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * RLARM + 4], v=s.rlarm.dy)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * RLARM + 5], v=s.rlarm.dth)


          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * LUARM], v=s.ruarm.x)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * LUARM + 1], v=s.ruarm.y)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * LUARM + 2], v=s.ruarm.th)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * LUARM + 3], v=s.ruarm.dx)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * LUARM + 4], v=s.ruarm.dy)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * LUARM + 5], v=s.ruarm.dth)

          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * LLARM], v=s.llarm.x)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * LLARM + 1], v=s.llarm.y)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * LLARM + 2], v=s.llarm.th)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * LLARM + 3], v=s.llarm.dx)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * LLARM + 4], v=s.llarm.dy)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * LLARM + 5], v=s.llarm.dth)


          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * HEAD], v=s.head.x)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * HEAD + 1], v=s.head.y)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * HEAD + 2], v=s.head.th)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * HEAD + 3], v=s.head.dx)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * HEAD + 4], v=s.head.dy)
          np.put(singleGame, ind=[singleIndex + NUM_STATES_PER * HEAD + 5], v=s.head.dth)

          np.reshape(singleGame,newshape=[cutoffTS + 1, NUM_STATES_PER * NUM_BODY_PARTS])

      allStatesInFile.append(singleGame)

  stateConcat = np.empty(shape=(0,NUM_STATES_PER * NUM_BODY_PARTS),dtype=np.float32)
  for states in allStatesInFile:
      stateConcat = np.concatenate((stateConcat, states), axis=0)

  print len(allStatesInFile)
  print len(allActionssInFile[5])
  return {'states' : allStatesInFile, 'actions' : allActionssInFile, 'concatState' : stateConcat, 'concatTS' : justTSToTransition}

def unison_shuffled_copies(a, b):
    assert len(a) == len(b)
    p = np.random.permutation(len(a))
    return a[p], b[p]

f = open("../../denseData_2017-11-06_08-58-03.proto", "rb")
data = extract_games(f)

## Convert to tensors and shuffle
# x_inputs_data = tf.convert_to_tensor(data['concatState'],dtype=tf.float32)
# x_shuffle = tf.random_shuffle(x_inputs_data, seed=8, name='shuffle_x')
# #random_normal([128, 1024], mean=0, stddev=1)
#
# y_inputs_data = tf.convert_to_tensor(np.reshape(np.asarray(data['concatTS']),(len(data['concatTS']),1)),dtype=tf.float32)
# y_shuffle = tf.random_shuffle(y_inputs_data, seed=8, name='shuffle_y')

x_shuffle, y_shuffle = unison_shuffled_copies(data['concatState'], np.reshape(np.asarray(data['concatTS']),(len(data['concatTS']),1)))


train_filename = 'denseData_2017-11-06_08-58-03.tfrecords'  # address to save the TFRecords file
# open the TFRecords file
writer = tf.python_io.TFRecordWriter(train_filename)

#TODO LOOK HERE https://github.com/tensorflow/tensorflow/blob/r1.4/tensorflow/core/example/feature.proto
# Create a feature
feature = {'train/label': x_shuffle,
           'train/image': y_shuffle}
# construct the Example proto boject
example = tf.train.Example(
    # Example contains a Features proto object
    features=tf.train.Features(
    # Features contains a map of string to Feature proto objects
          feature={
            # A Feature contains one of either a int64_list,
            # float_list, or bytes_list
            'label': tf.train.Feature(
                float_list=tf.train.FloatList(value=[x_shuffle])),
            'image': tf.train.Feature(
                float_list=tf.train.FloatList(value=[y_shuffle])),
    }))

# use the proto object to serialize the example to a string
serialized = example.SerializeToString()
# write the serialized object to disk
writer.write(serialized)

