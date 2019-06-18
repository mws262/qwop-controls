import tensorflow as tf
import os.path
import random
from tensorflow.python.client import timeline
from tensorflow.python.ops import rnn, rnn_cell
import sys
import numpy as np
import time
from tabulate import tabulate
import matplotlib.pyplot as plt

'''
PARAMETERS & SETTINGS
'''
# Note: if freeze_checkpoint.py won't give the nodes I need, add them to the outputs. DO NOT PUT A SPACE AROUND THE COMMAS IN THE NODE LIST.
# Additional troubles with trying to include input layer correct names -- somehow depends on iterator, which can't be loaded and initialized for some reason.
# Use tfrecord_input/Squeeze as input.
## python freeze_checkpoint.py --model_dir "./logs" --output_node_names "transform_out/unscaled_output,encoder/encoder_output"
## tensorboard --logdir=~/git/qwop_saveload/src/python/logs
tfrecordExtension = '.tfrecord'  # File extension for input datafiles. Datafiles must be TFRecord-encoded protobuf format.
tfrecordPath = '../src/main/resources/saved_data/training_data/''  # Location of datafiles on this machine. Beware of drive mounting locations.'
# On external drive ^. use sudo mount /dev/sdb1 /mnt OR /dev/sda2 for SSD

export_dir = './models/'
learn_rate = 1e-5

initWeightsStdev = .1

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
FUNCTIONS IN THE NN PIPELINED

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
    ts = context['TIMESTEPS']

    # Subtract out the x component of the body from all other x components.
    xoffsets = features[1]['BODY'][:,0] # Get first column.
    x_out_list = []
    for key in stateKeys:
        body_part = features[1][key]
        x_out_list.append(tf.reshape(body_part[:,0] - xoffsets,[-1,1]))
        x_out_list.append(body_part[:,1:])

    #feats = {key: features[1][key] for key in stateKeys}  # States
    states_concat = tf.concat(x_out_list, 1, name='concat_states')

    pressed_keys = tf.reshape(tf.cast(tf.decode_raw(features[1]['PRESSED_KEYS'], tf.uint8), dtype=tf.float32), [-1, 4])
    extended_states = (states_concat, pressed_keys, ts)

    #tf.concat([states_concat, pressed_keys], 1, name='concat_actions')

    # ttt = {'TIME_TO_TRANSITION': tf.reshape(tf.decode_raw(features[1]['TIME_TO_TRANSITION'], tf.uint8),)}
    # act = {'ACTIONS': tf.reshape(tf.decode_raw(features[1]['ACTIONS'], tf.uint8),(5,))}

    # feats.update({key: tf.reshape(tf.decode_raw(features[1][key], tf.uint8),(1,)) for key in actionKeys})  # Attach game.actions too after decoding.
    #feats.update(pk)
    return extended_states

def load_graph(frozen_graph_filename):
    # We load the protobuf file from the disk and parse it to retrieve the
    # unserialized graph_def
    with tf.gfile.GFile(frozen_graph_filename, "rb") as f:
        graph_def = tf.GraphDef()
        graph_def.ParseFromString(f.read())

    # Then, we import the graph_def into a new Graph and returns it
    with tf.Graph().as_default() as graph:
        # The name var will prefix every op/nodes in your graph
        # Since we load everything in a new graph, this is not needed
        tf.import_graph_def(graph_def)
    return graph

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

    initial = tf.truncated_normal(shape, stddev=initWeightsStdev)
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


def nn_layer(input_tensor, input_dim, output_dim, layer_name, act=tf.nn.leaky_relu):

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

def sequential_layers(input, layer_sizes, name_prefix, last_activation=tf.nn.leaky_relu):
    current_tensor = input
    for idx in range(len(layer_sizes) - 1):
        if idx == range(len(layer_sizes) - 1):
            current_tensor = nn_layer(current_tensor, layer_sizes[idx], layer_sizes[idx + 1], name_prefix + str(idx), act=last_activation)
        else:
            current_tensor = nn_layer(current_tensor, layer_sizes[idx], layer_sizes[idx + 1], name_prefix + str(idx))

    return current_tensor


def lstm(v_in, ts_in, s_in, name):
    with tf.variable_scope(name):
        cell = tf.nn.rnn_cell.BasicLSTMCell(num_units=s_in)
        rnn_outputs, last_states = tf.nn.dynamic_rnn(
            cell=cell,
            dtype=tf.float32,
            sequence_length=ts_in,
            inputs=v_in)
        return rnn_outputs, last_states


'''
DEFINE SPECIFIC DATAFLOW
'''
# batch_size = 64
print_freq = 9

frozen_model_filename = "./models/AutoEnc72_16_2_15_18.pb"
graph = load_graph(frozen_model_filename)

for op in graph.get_operations():
    print(op.name)

with tf.name_scope("dataset_input"):
    with graph.as_default():
        filenames = tf.placeholder(tf.string, shape=[None])
        dataset = tf.data.TFRecordDataset(filenames)
        dataset = dataset.map(_parse_function, num_parallel_calls=16)
        # dataset = dataset.shuffle(buffer_size=5000)
        dataset = dataset.repeat()
        # dataset = dataset.padded_batch(batch_size, padded_shapes=([None,72])) # Pad to max-length sequence
        iterator = dataset.make_initializable_iterator()
        next = iterator.get_next()
        state_batch = next[0]
        keys_batch = next[1]
        timesteps_batch = next[2]
        dataset = dataset.prefetch(256)


transform_in = graph.get_tensor_by_name('import/transform/transform_input:0')
tramsform_out = graph.get_tensor_by_name('import/transform/transform_output:0')

encoder_in = graph.get_tensor_by_name('import/encoder/encoder_input:0')
encoder_out = graph.get_tensor_by_name('import/encoder/encoder_output:0')

decoder_in = graph.get_tensor_by_name('import/decoder/decoder_input:0')
decoder_out = graph.get_tensor_by_name('import/decoder/decoder_output:0')

untransform_in = graph.get_tensor_by_name('import/untransform/untransform_input:0')
untransform_out = graph.get_tensor_by_name('import/untransform/untransform_output:0')

    #transform_out = graph.get_tensor_by_name('transform_out/Add_1:0')

with graph.as_default():
    with tf.name_scope('LSTM'):
        key_commands = tf.placeholder(tf.float32, shape=[None, 4], name='key_command_input')
        num_timesteps = tf.placeholder(tf.float32, shape=[], name='sequence_timesteps')
        encoded_state = tf.placeholder(tf.float32, shape=[None, 12], name='encoded_state')

        combined = tf.concat([encoded_state, key_commands], axis=1) # Insert the keystroke data here.
        rnn_outputs, _ = lstm(tf.expand_dims(combined, 0), [num_timesteps], 16, 'lstm1')
        rnn_outputs, _ = lstm(rnn_outputs, [num_timesteps], 16, 'lstm2')
        rnn_outputs = tf.squeeze(rnn_outputs)
        rnn_to_compressed = nn_layer(rnn_outputs, 16, 12, "fully_connected_rnn_reshaper")
        rnn_out = tf.identity(rnn_to_compressed, name="rnn_output")

    with tf.name_scope('loss'):
        loss_op = tf.losses.absolute_difference(encoded_state[:, 1:], rnn_out[:, :-1])

    with tf.name_scope('training'):
        adam = tf.train.AdamOptimizer(learn_rate)
        train_op = adam.minimize(loss_op, name='optimizer')

    saver = tf.train.Saver()

    # Create a summary to monitor cost tensor
    tf.summary.scalar('loss', loss_op)

    # Merge all summaries into a single op
    merged_summary_op = tf.summary.merge_all()



filename_list = []
for file in os.listdir(tfrecordPath):
    if file.endswith(tfrecordExtension):
        nextFile = tfrecordPath + file
        filename_list.append(nextFile)
        #print(nextFile)
random.shuffle(filename_list) # Shuffle so each time we restart, we get different order.

coord = tf.train.Coordinator()
with tf.Session(graph=graph) as sess:

    sess.run(tf.global_variables_initializer())

    if os.path.isfile("./logs/checkpoint"):
      ckpt = tf.train.get_checkpoint_state("./logs")
      print ckpt
      saver.restore(sess, ckpt.model_checkpoint_path)
      print('restored')

    sess.run(iterator.initializer, feed_dict={filenames: filename_list})
    # s,k,t = sess.run([state_batch,keys_batch,timesteps_batch])
    summary_writer = tf.summary.FileWriter("./logs", graph=tf.get_default_graph())

    for i in range(1000000):
        st, k, ts = sess.run([state_batch, keys_batch, timesteps_batch]) # Get a batch from tfrecords
        encoded = sess.run([encoder_out], feed_dict={transform_in: st}) # Transform and encode state
        rnn_comp, loss, _, summary = sess.run([rnn_to_compressed, loss_op, train_op, merged_summary_op], feed_dict={key_commands: k, num_timesteps: ts, encoded_state: np.squeeze(encoded)}) # Run through rnn
        recomposed = sess.run([untransform_out], feed_dict={decoder_in: np.squeeze(rnn_comp)})
        # loss, _ = sess.run([loss_op, train_op], feed_dict={pred: recomposed, true: st})
        if i%4 == 1:
            save_path = saver.save(sess, "./logs/rnn.ckpt", global_step=i)
            print("saved")

        print str(i) + "," + str(loss)
    # for i in range(100000000):
    #
    #     st_b, k_b, t_b = sess.run([state_batch, keys_batch, timesteps_batch])
    #     if i%print_freq == 0:
    #         #loss, _, summary, true_state, est_state, sca, me, decomp, ss = sess.run([loss_op, train_op, merged_summary_op, state_in, state_out, scaler_so_far, mean_so_far, decompressed_state, scaled_state], options=run_options,run_metadata = run_metadata) # est_state, true_state decompressed_state, full_state
    #         loss, _ = sess.run([loss_op, train_op], feed_dict={t_in: st_b, state_batch: st_b, timesteps_batch: t_b})
    #
    #
    #         print '\n' + '\033[1m'
    #         print("Iter: %d, Loss %f" % (i, loss))
    #         print '\033[0m'
    #         # save_path = saver.save(sess, "./logs/model2.ckpt", global_step=i)
    #     else:
    #         sess.run([train_op], feed_dict={t_in: st_b, state_batch: st_b, timesteps_batch: t_b})

