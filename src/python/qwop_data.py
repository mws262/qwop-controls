import numpy as np
import densedata_pb2 as dataset

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
      dat.ParseFromString(f.read())
      f.close
  except IOError:
      print "Import failed"

  ## Outer for loop here
  allStatesInFile = []
  allActionssInFile = []
  for gameIdx, game in enumerate(dat.denseData):
      num_timesteps = len(game.state)
      singleGame = np.zeros(shape=(num_timesteps, NUM_BODY_PARTS * NUM_STATES_PER), dtype=np.float32)
      for tsIdx, s in enumerate(game.state):

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

      allStatesInFile.append(singleGame)


      singleGameActions = {'q' : list(), 'w' : list(), 'o' : list(), 'p' : list(), 'ts_hold' : list(), 'ts_to_transition' : list()}
      for tsIdx, a in enumerate(game.action):
          singleGameActions['q'].append(a.Q)
          singleGameActions['w'].append(a.W)
          singleGameActions['o'].append(a.O)
          singleGameActions['p'].append(a.P)
          singleGameActions['ts_hold'].append(a.actionTimesteps)


          if tsIdx == 0: # For first index, we need to discover when the next transition is. Otherwise, we can just subtract one from the previously found one.
              prevTrans = 0
          else:
              prevTrans = singleGameActions['ts_to_transition'][-1]

          if prevTrans > 1:
                singleGameActions['ts_to_transition'].append(prevTrans - 1)
          else:
              tsToTransition = singleGameActions['ts_hold'][-1]
              singleGameActions['ts_to_transition'].append(tsToTransition)
      allActionssInFile.append(singleGameActions)




  return {'states' : allStatesInFile, 'actions' : allActionssInFile}

f = open("../../denseData_2017-11-06_08-58-03.proto", "rb")
data = extract_games(f)

  # class DataSet(object):
#
#   def __init__(self, states, labels):
#     """Construct a DataSet.
#     """
#     assert states.shape[0] == labels.shape[0], ('images.shape: %s labels.shape: %s' % (states.shape, labels.shape))
#     self._num_examples = states.shape[0]
#
#     ## DO RESCALING
#
#     self._states = states
#     self._labels = labels
#     self._epochs_completed = 0
#     self._index_in_epoch = 0
#
#   @property
#   def images(self):
#     return self._states
#
#   @property
#   def labels(self):
#     return self._labels
#
#   @property
#   def num_examples(self):
#     return self._num_examples
#
#   @property
#   def epochs_completed(self):
#     return self._epochs_completed
#
#   def next_batch(self, batch_size):
#       start = self._index_in_epoch
#
#       if start + batch_size > self._num_examples:
#           # Finished epoch
#           self._epochs_completed += 1
#           # Get the rest examples in this epoch
#           rest_num_examples = self._num_examples - start
#           states_rest_part = self._states[start:self._num_examples]
#           labels_rest_part = self._labels[start:self._num_examples]
#           # Start next epoch
#           start = 0
#           self._index_in_epoch = batch_size - rest_num_examples
#           end = self._index_in_epoch
#           states_new_part = self._states[start:end]
#           labels_new_part = self._labels[start:end]
#           return numpy.concatenate((states_rest_part, states_new_part), axis=0), numpy.concatenate(
#               (labels_rest_part, labels_new_part), axis=0)
#       else:
#           self._index_in_epoch += batch_size
#           end = self._index_in_epoch
#           return self._states[start:end], self._labels[start:end]
#
#
# def read_data_sets(train_dir, validation_size=5000):
#
#   validation_images = train_images[:validation_size]
#   validation_labels = train_labels[:validation_size]
#   train_images = train_images[validation_size:]
#   train_labels = train_labels[validation_size:]
#
#   train = DataSet(train_images, train_labels, **options)
#   validation = DataSet(validation_images, validation_labels, **options)
#   test = DataSet(test_images, test_labels, **options)
#
#   return datasets(train=train, validation=validation, test=test)
#
#
# def load_mnist(train_dir='MNIST-data'):
#   return read_data_sets(train_dir)