import numpy as np
import densedata_pb2 as dataset
import tensorflow as tf
import time
import timeit
import os.path
import math

## tensorboard --logdir=~/git/qwop_saveload/src/python/logs
## First: source ~/tensorflow/bin/activate

export_dir = './models/'

num_batches = 10
learn_rate = 1e-3

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

def read_and_decode_single_example(filename):
    # first construct a queue containing a list of filenames.
    # this lets a user split up there dataset in multiple files to keep
    # size down
    filename_queue = tf.train.string_input_producer([filename],
                                                    num_epochs=None)
    # Unlike the TFRecordWriter, the TFRecordReader is symbolic
    reader = tf.TFRecordReader()
    # One can read a single serialized example from a filename
    # serialized_example is a Tensor of type string.
    _, serialized_example = reader.read(filename_queue)
    # The serialized example is converted back to actual values.
    # One needs to describe the format of the objects to be returned
    features = tf.parse_single_example(
        serialized_example,
        features={
            # We know the length of both fields. If not the
            # tf.VarLenFeature could be used
            'x': tf.FixedLenFeature([72], tf.float32),
            'y': tf.FixedLenFeature([1], tf.float32)
        })
    # now return the converted data
    x = features['x']
    y = features['y']
    return x, y

def weight_variable(shape):
    """Create a weight variable with appropriate initialization."""
    initial = tf.truncated_normal(shape, stddev=0.1)
    return tf.Variable(initial)

def bias_variable(shape):
    """Create a bias variable with appropriate initialization."""
    initial = tf.constant(0.1, shape=shape)
    return tf.Variable(initial)

def variable_summaries(var):
  """Attach a lot of summaries to a Tensor (for TensorBoard visualization)."""
  with tf.name_scope('summaries'):
    mean = tf.reduce_mean(var)
    tf.summary.scalar('mean', mean)
    with tf.name_scope('stddev'):
      stddev = tf.sqrt(tf.reduce_mean(tf.square(var - mean)))
    tf.summary.scalar('stddev', stddev)
    tf.summary.scalar('max', tf.reduce_max(var))
    tf.summary.scalar('min', tf.reduce_min(var))
    tf.summary.histogram('histogram', var)

def nn_layer(input_tensor, input_dim, output_dim, layer_name, act=tf.nn.relu):
  """Reusable code for making a simple neural net layer.

  It does a matrix multiply, bias add, and then uses relu to nonlinearize.
  It also sets up name scoping so that the resultant graph is easy to read,
  and adds a number of summary ops.
  """
  # Adding a name scope ensures logical grouping of the layers in the graph.
  with tf.name_scope(layer_name):
    # This Variable will hold the state of the weights for the layer
    with tf.name_scope('weights'):
      weights = weight_variable([input_dim, output_dim])
      variable_summaries(weights)
    with tf.name_scope('biases'):
      biases = bias_variable([output_dim])
      variable_summaries(biases)
    with tf.name_scope('Wx_plus_b'):
      preactivate = tf.matmul(input_tensor, weights) + biases
      tf.summary.histogram('pre_activations', preactivate)
    activations = act(preactivate, name='activation')
    tf.summary.histogram('activations', activations)
    return activations

#builder = tf.saved_model.builder.SavedModelBuilder(export_dir)

x, y = read_and_decode_single_example("denseData_2017-11-06_08-58-03.tfrecords")

# f = open("../../denseData_2017-11-06_08-58-03.proto", "rb")
# data = extract_games(f)
#
# ## Convert to tensors and shuffle
# x_inputs_data = tf.convert_to_tensor(data['concatState'],dtype=tf.float32)
# x_shuffle = tf.random_shuffle(x_inputs_data, seed=8, name='shuffle_x')
# #random_normal([128, 1024], mean=0, stddev=1)
#
# y_inputs_data = tf.convert_to_tensor(np.reshape(np.asarray(data['concatTS']),(len(data['concatTS']),1)),dtype=tf.float32)
# y_shuffle = tf.random_shuffle(y_inputs_data, seed=8, name='shuffle_y')
#
# # TODO subtract mean, div by stdev?
# # tf.nn.l2_normalize(
# #     tf.convert_to_tensor(data['concatState'],dtype=tf.float32)
# #     ,0) #tf.
# # tf.nn.l2_normalize(
# # tf.convert_to_tensor(np.reshape(np.asarray(data['concatTS']),(len(data['concatTS']),1)),dtype=tf.float32)
# # ,0)
#
# ## SPLIT DATA INTO BATCHES
# batch_size = math.floor(len(data['concatTS'])/num_batches)
# batches = np.ones(num_batches, dtype=np.int32) * batch_size
# batches = batches.tolist()
# batches[-1] = len(data['concatTS']) - (num_batches - 1) * batch_size # Last element takes whatever is left.
# batches = [int(i) for i in batches]
# x_batch = tf.split(x_shuffle, num_or_size_splits=batches, name='split_x')
# y_batch = tf.split(y_shuffle, num_or_size_splits=batches, name='split_y')


# ## VALIDATION DATA
# fValidate = open("../../denseData_2017-11-07_10-31-05.proto", "rb")
# dataValidate = extract_games(fValidate)
#
# ## Convert to tensors and shuffle
# x_valid_data = tf.convert_to_tensor(data['concatState'],dtype=tf.float32)
# #random_normal([128, 1024], mean=0, stddev=1)
#
# y_valid_data = tf.convert_to_tensor(np.reshape(np.asarray(data['concatTS']),(len(data['concatTS']),1)),dtype=tf.float32)







# We build our small model: a basic two layers neural net with ReLU
with tf.name_scope('input'):
    x = tf.placeholder(tf.float32, shape=[None, 72], name='x-input')
    y_true = tf.placeholder(tf.int32, shape=[None, 1], name='y-input')


layer1 = nn_layer(x, 72, 144, 'layer1')

with tf.name_scope('dropout'):
    keep_prob = tf.placeholder(tf.float32)
    tf.summary.scalar('dropout_keep_probability', keep_prob)
    dropped = tf.nn.dropout(layer1, keep_prob)

layer2 = nn_layer(dropped, 144, 72, 'layer2')

y = nn_layer(layer2, 72, 1, 'layer3')


with tf.name_scope('Loss'):
    loss_op = tf.losses.mean_squared_error(y_true, y)
with tf.name_scope('Accuracy'):
    accuracy = tf.reduce_mean(tf.cast(tf.equal(tf.cast(tf.round(y),tf.int32),tf.cast(y_true,tf.int32)),tf.float32))



np.set_printoptions(threshold=np.nan) # Makes printing not be truncated
# We add the training operation, ...
adam = tf.train.AdamOptimizer(learn_rate)
train_op = adam.minimize(loss_op, name="optimizer")

# Add ops to save and restore all the variables -- checkpoint style
saver = tf.train.Saver()

# Create a summary to monitor cost tensor
tf.summary.scalar("loss", loss_op)
tf.summary.scalar("accuracy", accuracy)
# Merge all summaries into a single op
merged_summary_op = tf.summary.merge_all()


startTime = time.time()
with tf.Session() as sess:
    # ... init our variables, ...
    sess.run(tf.global_variables_initializer())
    tf.train.start_queue_runners(sess=sess)

    # #if os.path.isfile("./tmp/model.ckpt"):
    # saver.restore(sess, "./tmp/model.ckpt")
    # print('Loaded checkpoint file')
    #builder.add_meta_graph_and_variables(sess)

    summary_writer = tf.summary.FileWriter("./logs", graph=tf.get_default_graph())
    # ... train ...
    for i in range(2000):
        print x
        x_input, y_input = sess.run([x, y])

       # print x_input
        _, loss, acc, summary = sess.run([train_op, loss_op, accuracy, merged_summary_op],feed_dict={x: x_input, y_true: y_input, keep_prob: 0.9})
        print('iter:%d - loss:%f - accuracy:%f' % (i, loss, acc))
        # for xin,yin in zip(x_batch,y_batch):
        #     x_input, y_input = sess.run([xin, yin])
        #     # ... and feeding it to our model
        #     _, loss, acc, summary = sess.run([train_op, loss_op, accuracy, merged_summary_op], feed_dict={x: x_input, y_true: y_input, keep_prob : 0.9})
        #
        #     # summary_writer.add_summary(summary, i)
        #
        #     # We regularly check the loss
        #     #if i % 500 == 0:
        #     print('iter:%d - loss:%f - accuracy:%f' % (i, loss, acc))
        #
        # if i % 10 == 9:
        #     save_path = saver.save(sess, "./tmp/model.ckpt")
        #     # Finally, we check our final accuracy
        # #     x_input, y_input = sess.run([x_inputs_data, y_inputs_data])
        # #     print sess.run(y, feed_dict={
        # #     x: x_input,
        # #     y_true: y_input,
        # #     keep_prob: 1.0
        # # })


    # Finally, we check our final accuracy
    x_input, y_input = sess.run([x_inputs_data, y_inputs_data])
    sess.run(accuracy, feed_dict={
        x: x_input,
        y_true: y_input,
        keep_prob: 1
    })
   # builder.save()

print("Time taken: %f" % (time.time() - startTime))