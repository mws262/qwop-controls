import tensorflow as tf
import numpy as np
import os.path
from tensorflow.python.ops import rnn, rnn_cell

'''
PARAMETERS & SETTINGS
'''

tfrecordExtension = '.tfrecord'  # File extension for input datafiles. Datafiles must be TFRecord-encoded protobuf format.
tfrecordPath = '/mnt/QWOP_Tfrecord_1_20/'  # Location of datafiles on this machine. Beware of drive mounting locations.

export_dir = './models/'
learn_rate = 1e-3

initWeightsStdev = 0.1
initBiasVal = 0.1

# All states found in the TFRECORD files
stateKeys = ['BODY', 'HEAD', 'RTHIGH', 'LTHIGH', 'RCALF', 'LCALF',
             'RFOOT', 'LFOOT', 'RUARM', 'LUARM', 'RLARM', 'LLARM']

# Various action parameterizations put in the TFRECORD files
actionKeys = ['PRESSED_KEYS', 'TIME_TO_TRANSITION', 'ACTIONS']

# Various information pertaining to the ENTIRE run, but not recorded at every timestep
contextKeys = ['TIMESTEPS']

# Tensorflow placeholders for for sequence and action features as defined by the stateKeys and actionKeys above.
sequence_features = {skey: tf.FixedLenSequenceFeature([6], tf.float32, True) for skey in stateKeys}
sequence_features.update({akey: tf.FixedLenSequenceFeature([], tf.string, True) for akey in actionKeys})
context_features = {ckey: tf.FixedLenFeature([],tf.int64,True) for ckey in contextKeys}


'''
FUNCTIONS IN THE NN PIPELINE
'''

def _parse_function(example_proto):
    # The serialized example is converted back to actual values.
    features = tf.parse_single_sequence_example(
        serialized=example_proto,
        context_features=context_features,
        sequence_features=sequence_features
    )
    context = features[0]  # Total number of timesteps in here with key 'TIMESTEPS'
    ts = context['TIMESTEPS']
    ts_padded = tf.fill([1,], ts, name='timesteps_padded')
    feats = {key: features[1][key] for key in stateKeys}  # States

    statesConcat = tf.concat(feats.values(),1)

    # pk = {'PRESSED_KEYS': tf.reshape(tf.decode_raw(features[1]['PRESSED_KEYS'], tf.uint8), [-1, 4])}
    # ttt = {'TIME_TO_TRANSITION': tf.reshape(tf.decode_raw(features[1]['TIME_TO_TRANSITION'], tf.uint8),)}
    # act = {'ACTIONS': tf.reshape(tf.decode_raw(features[1]['ACTIONS'], tf.uint8),(5,))}

    # feats.update({key: tf.reshape(tf.decode_raw(features[1][key], tf.uint8),(1,)) for key in actionKeys})  # Attach actions too after decoding.
    #feats.update(pk)
    return statesConcat


def weight_variable(shape):
    """
    Initialize weight variables for a net layer.

    :param shape: Shape of tensor to create.
    :return: Tensor of weight variables initialized randomly.
    """

    initial = tf.truncated_normal(shape, stddev=initWeightsStdev)
    return tf.Variable(initial)


def bias_variable(shape):
    """
    Net layer bias values.

    :param shape: Shape of tensor to create.
    :return: Bias tensor initialized to a constant value.
    """

    initial = tf.constant(initBiasVal, shape=shape)
    return tf.Variable(initial)


def variable_summaries(var):
    """
    Attach a lot of summaries to a Tensor (for TensorBoard visualization).

    :param var:
    :return: None
    """

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

    :param input_tensor:
    :param input_dim:
    :param output_dim:
    :param layer_name:
    :param act:
    :return:
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

def lstm_layer(input_tensor, input_dim, output_dim, layer_name):

    # Adding a name scope ensures logical grouping of the layers in the graph.
    with tf.name_scope(layer_name):
        # This Variable will hold the state of the weights for the layer
        with tf.name_scope('weights'):
            weights = weight_variable([input_dim, output_dim])
            variable_summaries(weights)
        with tf.name_scope('biases'):
            biases = bias_variable([output_dim])
            variable_summaries(biases)
        with tf.name_scope('LSTM'):
            lstm_cell = rnn_cell.BasicLSTMCell(input_dim, state_is_tuple=True)
            outputs, states = rnn.static_rnn(lstm_cell, input_tensor, dtype=tf.float32)
        with tf.name_scope('Wx_plus_b'):
            activations = tf.matmul(outputs[-1], weights) + biases
        return activations

'''
DEFINE SPECIFIC DATAFLOW
'''

# Make a list of TFRecord files.
filename_list = []
for file in os.listdir(tfrecordPath):
    if file.endswith(tfrecordExtension):
        nextFile = tfrecordPath + file
        filename_list.append(nextFile)
        print(nextFile)

filenames = tf.placeholder(tf.string, shape=[None])
dataset = tf.data.TFRecordDataset(filenames)
dataset = dataset.map(_parse_function)
dataset = dataset.shuffle(buffer_size=5000)
dataset = dataset.repeat()  # Repeat the input indefinitely.
dataset = dataset.batch(1)  # .padded_batch(4, padded_shapes=[None])
iterator = dataset.make_initializable_iterator()

next_element = iterator.get_next()

# print('%d files in queue.' % len(filename_list))


# LAYERS
n_output = 72 # 72 state vector elements + 4 key presses.
n_input = 76 # (I think) How many previous steps are fed into to get the prediction. This is the short-term memory part of LSTM.
n_hidden = 144 # (I think) How many hidden state variables are passed from timestep to timestep.

# Input layer.
with tf.name_scope('input'):
    qwop_state = tf.placeholder(tf.float32, shape=[None,72], name='qwop-state-input')
    qwop_action = tf.placeholder(tf.float32, shape=[None,4], name='qwop-action-input')
    init_net_state = tf.placeholder(tf.int32, shape=[None, 1], name='action-input')


combined_qwop_state = tf.stack([qwop_state, qwop_action])

rnn_cell = rnn.BasicLSTMCell(n_hidden)
outputs, state = rnn.static_rnn(cell=rnn_cell,inputs=combined_qwop_state)


# num_hidden = 24
# data = tf.placeholder(tf.float32, [None, 20,1])
# target = tf.placeholder(tf.float32, [None, 21])
# cell = tf.nn.rnn_cell.LSTMCell(num_hidden,state_is_tuple=True)
# val, state = tf.nn.dynamic_rnn(cell, data, dtype=tf.float32)
#
#
# with tf.name_scope('Loss'):
#     loss_op = tf.losses.mean_squared_error(state, layer4)


    ################
    #
    # # tf Graph input
    # X = tf.placeholder("float", [None, timesteps, 72])
    #
    # # Define weights
    # weights = {
    #     'out': tf.Variable(tf.random_normal([num_hidden, num_classes]))
    # }
    # biases = {
    #     'out': tf.Variable(tf.random_normal([num_classes]))
    # }
    #
    #
    # def RNN(x, weights, biases):
    #     # Prepare data shape to match `rnn` function requirements
    #     # Current data input shape: (batch_size, timesteps, n_input)
    #     # Required shape: 'timesteps' tensors list of shape (batch_size, n_input)
    #
    #     # Unstack to get a list of 'timesteps' tensors of shape (batch_size, n_input)
    #     x = tf.unstack(x, timesteps, 1)
    #
    #     # Define a lstm cell with tensorflow
    #     lstm_cell = rnn.BasicLSTMCell(num_hidden, forget_bias=1.0)
    #
    #     # Get lstm cell output
    #     outputs, states = rnn.static_rnn(lstm_cell, x, dtype=tf.float32)
    #
    #     # Linear activation, using rnn inner loop last output
    #     return tf.matmul(outputs[-1], weights['out']) + biases['out']
    #

# Training operation.
adam = tf.train.AdamOptimizer(learn_rate)
train_op = adam.minimize(loss_op, name="optimizer")


# FINAL SETUP

# Add ops to save and restore all the variables -- checkpoint style
saver = tf.train.Saver()

# Create a summary to monitor cost tensor
tf.summary.scalar("loss", loss_op)

# Merge all summaries into a single op
merged_summary_op = tf.summary.merge_all()


coord = tf.train.Coordinator() # Can kill everything when code is done. Prevents the `Skipping cancelled enqueue attempt with queue not closed'

'''
EXECUTE NET
'''


with tf.Session() as sess:
    # Initialize all variables.
    sess.run(tf.global_variables_initializer())

    if os.path.isfile("./tmp/model.ckpt"):
      saver.restore(sess, "./tmp/model.ckpt")

    threads = tf.train.start_queue_runners(sess=sess, coord=coord)

    sess.run(iterator.initializer, feed_dict={filenames: filename_list})

    for i in range(10000000):
        state_next = sess.run([next_element])
        loss, _ = sess.run([loss_op,train_op], feed_dict={state: np.squeeze(state_next)})
        if i%99 == 0:
            print("Iter: %d, Loss %f\n", [i,loss])
            save_path = saver.save(sess, "./tmp/model1.ckpt")
    # print np.shape(sess.run([next_element]))


    # for i in range(100000):
    #
    #     state_input = sess.run([state_in])
    #     layer1_out, loss_out, _ = sess.run([layer1,loss_op,train_op], feed_dict={state: np.squeeze(state_input)})
    #
    #     print('iter:%d - loss:%f' % (i, loss_out))

    # Stop all the queue threads.
    # coord.request_stop()
    # coord.join(threads)
