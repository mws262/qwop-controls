import tensorflow as tf
import numpy as np
import os.path
from tensorflow.python.ops import rnn, rnn_cell
import matplotlib.pyplot as plt

'''
PARAMETERS & SETTINGS
'''

tfrecordExtension = '.TFRecord'  # File extension for input datafiles. Datafiles must be TFRecord-encoded protobuf format.
tfrecordPath = '../src/main/resources/saved_data/training_data/'  # Location of datafiles on this machine. Beware of drive mounting locations.

export_dir = './models/'
learn_rate = 1e-5

initWeightsStdev = 0.1
initBiasVal = 0.1

# All states found in the TFRECORD files
stateKeys = ['BODY', 'HEAD', 'RTHIGH', 'LTHIGH', 'RCALF', 'LCALF',
             'RFOOT', 'LFOOT', 'RUARM', 'LUARM', 'RLARM', 'LLARM']

# Various action parameterizations put in the TFRECORD files
actionKeys = ['PRESSED_KEYS', 'PRESSED_KEYS_ONE_HOT', 'TIME_TO_TRANSITION', 'ACTIONS']

# Various information pertaining to the ENTIRE run, but not recorded at every timestep
contextKeys = ['TIMESTEPS']

# Tensorflow placeholders for for sequence and action features as defined by the stateKeys and actionKeys above.
sequence_features = {skey: tf.FixedLenSequenceFeature([6], tf.float32, True) for skey in stateKeys}
sequence_features.update({akey: tf.FixedLenSequenceFeature([], tf.string, True) for akey in actionKeys})
context_features = {ckey: tf.FixedLenFeature([], tf.int64, True) for ckey in contextKeys}

'''
FUNCTIONS IN THE NN PIPELINE
'''


def _parse_function(example_proto):
    # The serialized example is converted back to actual values.
    features = tf.parse_single_sequence_example(
        serialized=example_proto,
        context_features=context_features,
        sequence_features=sequence_features,
        name='parse_sequence'
    )
    context = features[0]  # Total number of timesteps in here with key 'TIMESTEPS'
    timesteps = context['TIMESTEPS']
    # feats = {key: features[1][key] for key in stateKeys}  # States
    xoffsets = features[1]['BODY'][:, 0]  # Get first column.
    x_out_list = []
    for key in stateKeys:
        body_part = features[1][key]
        x_out_list.append(tf.reshape(body_part[:, 0] - xoffsets, [-1, 1]))
        x_out_list.append(body_part[:, 1:])

    statesConcat = tf.concat(x_out_list, 1, name='concat_states')

    # pressedKeys = tf.reshape(tf.decode_raw(features[1]['PRESSED_KEYS'], tf.uint8), [-1, 4])
    pressedKeyClassification = tf.cast(
        tf.reshape(tf.decode_raw(features[1]['PRESSED_KEYS_ONE_HOT'], tf.uint8), [-1, 3]), dtype=tf.float32)

    # ttt = tf.reshape(tf.decode_raw(features[1]['TIME_TO_TRANSITION'], tf.uint8),)
    # act = tf.reshape(tf.decode_raw(features[1]['ACTIONS'], tf.uint8),(5,))

    return timesteps, statesConcat, pressedKeyClassification


def weight_variable(shape):
    """
    Initialize weight variables for a net layer.

    :param shape: Shape of tensor to create.
    :return: Tensor of weight variables initialized randomly.
    """

    initial = tf.truncated_normal(shape, stddev=initWeightsStdev, name='weight')
    return tf.Variable(initial)


def bias_variable(shape):
    """
    Net layer bias values.

    :param shape: Shape of tensor to create.
    :return: Bias tensor initialized to a constant value.
    """

    initial = tf.constant(initBiasVal, shape=shape, name='bias')
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


def sequential_layers(input, layer_sizes, name_prefix, last_activation=tf.nn.leaky_relu):
    current_tensor = input
    for idx in range(len(layer_sizes) - 1):
        if idx == range(len(layer_sizes) - 1):
            current_tensor = nn_layer(current_tensor, layer_sizes[idx], layer_sizes[idx + 1], name_prefix + str(idx),
                                      act=last_activation)
        else:
            current_tensor = nn_layer(current_tensor, layer_sizes[idx], layer_sizes[idx + 1], name_prefix + str(idx))

    return current_tensor


def _create_one_cell():
    return tf.contrib.rnn.LSTMCell(72, state_is_tuple=True, name='rnn_cell')
    # if config.keep_prob < 1.0:
    #     return tf.contrib.rnn.DropoutWrapper(lstm_cell, output_keep_prob=config.keep_prob)


'''
DEFINE SPECIFIC DATAFLOW
'''

file = open("saved_normalization.info", 'r')
norm_data = np.load(file)
data_mins = tf.convert_to_tensor(norm_data['min'], dtype=tf.float32)
data_ranges = tf.convert_to_tensor(norm_data['range'] + 1e-6, dtype=tf.float32)

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
dataset = dataset.shuffle(buffer_size=100)
dataset = dataset.repeat()  # Repeat the input indefinitely.
dataset = dataset.batch(1)  # .padded_batch(4, padded_shapes=[None])
iterator = dataset.make_initializable_iterator()

next_element = iterator.get_next()

print('%d files in queue.' % len(filename_list))

# LAYERS
global_step = tf.Variable(0)

#
# Input layer.
with tf.name_scope('input'):
    # sequence_length = tf.placeholder(tf.int32, shape=[1], name='run-timestep-count')
    qwop_state = tf.placeholder(tf.float32, shape=[None, None, 72], name='qwop_state_input')
    qwop_state_tform = tf.divide(tf.subtract(qwop_state, data_mins, name='subtract_data_mean'), data_ranges, name='divide_data_range')

    qwop_action = tf.placeholder(tf.float32, shape=[None, None, 3], name='qwop_action_input')
    extended_state = tf.concat([qwop_state_tform, qwop_action], axis=2, name='concat_state_action')

with tf.name_scope('rnn'):
    # rnn_cell = tf.nn.rnn_cell.BasicLSTMCell(n_hidden)

    layers = 2;
    rnn_cell = tf.contrib.rnn.MultiRNNCell(
        [_create_one_cell() for _ in range(layers)],
        state_is_tuple=True
    ) if layers > 1 else _create_one_cell()
    rnn_internal_state_input = rnn_cell.zero_state(1, tf.float32)

    # This is a stupid way of making placeholders for the internal state of the RNN. As far as I know, this is the "official" way of doing it >_<.

    full_internal_state_input = tf.placeholder_with_default(tf.zeros(shape=[layers,2,1,72], dtype=tf.float32), shape=[layers,2,1,72], name='full_internal_state_input')

    c_state1 = tf.placeholder_with_default(tf.reshape(full_internal_state_input[0,0,0,:], shape=[1,72]), shape=[1,72], name='internal_state_c1')
    h_state1 = tf.placeholder_with_default(tf.reshape(full_internal_state_input[0,1,0,:], shape=[1,72]), shape=[1,72], name='internal_state_h1')
    c_state2 = tf.placeholder_with_default(tf.reshape(full_internal_state_input[min(1, layers-1),0,0,:], shape=[1,72]), shape=[1,72], name='internal_state_c2')
    h_state2 = tf.placeholder_with_default(tf.reshape(full_internal_state_input[min(1, layers-1),1,0,:], shape=[1,72]), shape=[1,72], name='internal_state_h2')
    c_state3 = tf.placeholder_with_default(tf.reshape(full_internal_state_input[min(2, layers-1),0,0,:], shape=[1,72]), shape=[1,72], name='internal_state_c3')
    h_state3 = tf.placeholder_with_default(tf.reshape(full_internal_state_input[min(2, layers-1),1,0,:], shape=[1,72]), shape=[1,72], name='internal_state_h3')
    c_state4 = tf.placeholder_with_default(tf.reshape(full_internal_state_input[min(3, layers-1), 0, 0, :], shape=[1, 72]),
                                           shape=[1, 72], name='internal_state_c4')
    h_state4 = tf.placeholder_with_default(tf.reshape(full_internal_state_input[min(3, layers-1), 1, 0, :], shape=[1, 72]),
                                           shape=[1, 72], name='internal_state_h4')

    init_st1 = tf.nn.rnn_cell.LSTMStateTuple(c_state1, h_state1)
    init_st2 = tf.nn.rnn_cell.LSTMStateTuple(c_state2, h_state2)
    init_st3 = tf.nn.rnn_cell.LSTMStateTuple(c_state3, h_state3)
    init_st4 = tf.nn.rnn_cell.LSTMStateTuple(c_state4, h_state4)
    rnn_internal_state_input = (init_st1, init_st2, init_st3, init_st4)[0:layers]
    game_state_from_rnn, rnn_internal_state_output = rnn.dynamic_rnn(cell=rnn_cell, inputs=extended_state,
                                                                     initial_state=rnn_internal_state_input,
                                                                     dtype=tf.float32)


with tf.name_scope('output'):
    predicted_state_out = tf.add(tf.multiply(game_state_from_rnn, data_ranges, name='rescale_by_data_range'),
                                 data_mins, name='reoffset_by_data_min')
    state_out = tf.identity(predicted_state_out, name='state_output')
    internal_state_out = tf.identity(rnn_internal_state_output, name='internal_state_output')

with tf.name_scope('loss'):
    # loss_op = tf.nn.softmax_cross_entropy_with_logits_v2(logits=softmax_out, labels=qwop_action)
    # reducedLoss = tf.reduce_mean(loss_op)
    loss_op = tf.losses.mean_squared_error(qwop_state_tform, game_state_from_rnn)
    reducedLoss = tf.reduce_mean(loss_op, name='single_number_loss')

with tf.name_scope('training'):
    optim = tf.train.RMSPropOptimizer(learn_rate, name='optimizer')
    train_op = optim.minimize(loss_op, global_step=global_step, name='train_op')
    # adam = tf.train.AdamOptimizer(learn_rate)
    # train_op = adam.minimize(loss_op, global_step=global_step, name='optimizer')

saver = tf.train.Saver()
tf.summary.scalar('loss', loss_op)
merged_summary_op = tf.summary.merge_all()
np.set_printoptions(threshold=np.nan)

coord = tf.train.Coordinator()  # Can kill everything when code is done. Prevents the `Skipping cancelled enqueue attempt with queue not closed'

# config = tf.ConfigProto(
#     device_count={'GPU': 1}
# )

'''
EXECUTE NET
'''
train = True
with tf.Session() as sess:
    # Initialize all variables.
    sess.run(tf.global_variables_initializer())

    if os.path.isfile("./logs/checkpoint"):
        ckpt = tf.train.get_checkpoint_state("./logs")
        saver.restore(sess, ckpt.model_checkpoint_path)
        print('restored')

    sess.run(iterator.initializer, feed_dict={filenames: filename_list})

    # Do some testing and plotting rather than training.
    if not train:
        for q in range(100):
            state_next = sess.run([next_element])

            st_in = np.expand_dims(state_next[0][1][0][0], axis=0)
            st_in = np.expand_dims(st_in, axis=0)

            act_in = np.expand_dims(state_next[0][2][0][0], axis=0)
            act_in = np.expand_dims(act_in, axis=0)

            rnn_out, int_st, next_st = sess.run([game_state_from_rnn, rnn_internal_state_output, predicted_state_out],
                                                feed_dict={qwop_state: st_in,
                                                           qwop_action: act_in})

            pred_sim = np.zeros(shape=[int(state_next[0][0]), 72])
            actual = state_next[0][1][0]
            pred_sim[0, :] = st_in
            pred_sim[1, :] = next_st
            for i in range(2, state_next[0][0]):
                # st_in = np.expand_dims(state_next[0][1][0][i], axis=0)
                # st_in = np.expand_dims(st_in, axis=0)

                act_in = np.expand_dims(state_next[0][2][0][i], axis=0)
                act_in = np.expand_dims(act_in, axis=0)
                rnn_out, int_st, next_st = sess.run([game_state_from_rnn, rnn_internal_state_output, predicted_state_out],
                                                    feed_dict={qwop_state: next_st, qwop_action: act_in,
                                                               rnn_internal_state_input: int_st})
                pred_sim[i, :] = next_st

            plot_end = int(state_next[0][0])
            plt.subplot(2, 1, 1)
            plt.plot(range(plot_end), pred_sim[0:plot_end, 1:6])

            plt.subplot(2, 1, 2)
            plt.plot(range(plot_end), actual[0:plot_end, 1:6])
            plt.show()
    else:
        # Do training for an arbitrarily long time.
        for i in range(100000):
            state_next = sess.run([next_element])
            loss, _, meanLoss = sess.run([loss_op, train_op, reducedLoss],
                                         feed_dict={qwop_state: state_next[0][1],
                                                    qwop_action: state_next[0][2]})
            print(meanLoss)

            if i % 10 == 0:
                save_path = saver.save(sess, "./logs/model.ckpt", global_step=i)
