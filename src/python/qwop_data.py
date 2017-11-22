import numpy as np
import densedata_pb2 as dataset
import tensorflow as tf
import time

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
  justTSToTransition = []
  for gameIdx, game in enumerate(dat.denseData):
      num_timesteps = len(game.state)

      #### ACTIONS ####
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
                justTSToTransition.append(prevTrans - 1)
          else:
              tsToTransition = singleGameActions['ts_hold'][-1]
              singleGameActions['ts_to_transition'].append(tsToTransition)
              justTSToTransition.append(prevTrans - 1)
      allActionssInFile.append(singleGameActions)

      #### STATES ####
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

          np.reshape(singleGame,newshape=[num_timesteps,NUM_STATES_PER * NUM_BODY_PARTS])

      allStatesInFile.append(singleGame)


  print len(allStatesInFile)
  print len(allActionssInFile[5])
  return {'states' : allStatesInFile, 'actions' : allActionssInFile, 'justTS' : justTSToTransition}


f = open("../../denseData_2017-11-06_08-58-03.proto", "rb")
data = extract_games(f)
"""
x_inputs_data = data['states'] #tf.random_normal([128, 1024], mean=0, stddev=1)
# We will try to predict this law:
# predict 1 if the sum of the elements is positive and 0 otherwise
y_inputs_data = data['justTS']

# We build our small model: a basic two layers neural net with ReLU
with tf.variable_scope("placeholder"):
    input = tf.placeholder(tf.float32, shape=[None, 72])
    y_true = tf.placeholder(tf.int32, shape=[None, 1])
with tf.variable_scope('FullyConnected'):
    w = tf.get_variable('w', shape=[72, 72], initializer=tf.random_normal_initializer(stddev=1e-1))
    b = tf.get_variable('b', shape=[72], initializer=tf.constant_initializer(0.1))
    z = tf.matmul(input, w) + b
    y = tf.nn.relu(z)

    w2 = tf.get_variable('w2', shape=[72, 1], initializer=tf.random_normal_initializer(stddev=1e-1))
    b2 = tf.get_variable('b2', shape=[1], initializer=tf.constant_initializer(0.1))
    z = tf.matmul(y, w2) + b2
with tf.variable_scope('Loss'):
    losses = tf.nn.sigmoid_cross_entropy_with_logits(None, tf.cast(y_true, tf.float32), z)
    loss_op = tf.reduce_mean(losses)
with tf.variable_scope('Accuracy'):
    y_pred = tf.cast(z > 0, tf.int32)
    accuracy = tf.reduce_mean(tf.cast(tf.equal(y_pred, y_true), tf.float32))
    accuracy = tf.Print(accuracy, data=[accuracy], message="accuracy:")

# We add the training operation, ...
adam = tf.train.AdamOptimizer(1e-2)
train_op = adam.minimize(loss_op, name="train_op")

startTime = time.time()
with tf.Session() as sess:
    # ... init our variables, ...
    sess.run(tf.global_variables_initializer())

    # ... check the accuracy before training, ...
    x_input, y_input = sess.run([x_inputs_data, y_inputs_data])
    sess.run(accuracy, feed_dict={
        input: x_input,
        y_true: y_input
    })

    # ... train ...
    for i in range(5000):
        #  ... by sampling some input data (fetching) ...
        x_input, y_input = sess.run([x_inputs_data, y_inputs_data])
        # ... and feeding it to our model
        _, loss = sess.run([train_op, loss_op], feed_dict={
            input: x_input,
            y_true: y_input
        })

        # We regularly check the loss
        if i % 500 == 0:
            print('iter:%d - loss:%f' % (i, loss))

    # Finally, we check our final accuracy
    x_input, y_input = sess.run([x_inputs_data, y_inputs_data])
    sess.run(accuracy, feed_dict={
        input: x_input,
        y_true: y_input
    })

print("Time taken: %f" % (time.time() - startTime))
"""