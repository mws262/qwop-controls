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
#freeze_checkpoint.py --model_dir "./logs" --output_node_names "transform_out/Add_1" --blacklist_nodes "transform_in/transform_input"
## tensorboard --logdir=~/git/qwop_saveload/src/python/logs
tfrecordExtension = '.tfrecord'  # File extension for input datafiles. Datafiles must be TFRecord-encoded protobuf format.
tfrecordPath = '/mnt/QWOP_Tfrecord_1_20/'  # Location of datafiles on this machine. Beware of drive mounting locations.
# On external drive ^. use sudo mount /dev/sdb1 /mnt

export_dir = './models/'
learn_rate = 1e-4

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
    statesConcat = tf.concat(x_out_list, 1, name='concat_states')

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



'''
DEFINE SPECIFIC DATAFLOW
'''
batch_size = 64
print_freq = 9

# Make a list of TFRecord files.
filename_list = []
for file in os.listdir(tfrecordPath):
    if file.endswith(tfrecordExtension):
        nextFile = tfrecordPath + file
        filename_list.append(nextFile)
        print(nextFile)
random.shuffle(filename_list) # Shuffle so each time we restart, we get different order.

filenames = tf.placeholder(tf.string, shape=[None])
dataset = tf.data.TFRecordDataset(filenames)
dataset = dataset.map(_parse_function, num_parallel_calls=16)
#dataset = dataset.shuffle(buffer_size=5000)
dataset = dataset.repeat()
#dataset = dataset.padded_batch(batch_size, padded_shapes=([None,72])) # Pad to max-length sequence
#dataset = dataset.apply(tf.contrib.data.unbatch())
iterator = dataset.make_initializable_iterator()
next = iterator.get_next()
# next = tf.layers.batch_normalization(next, axis=1,center=True, scale=True, training=True)
next_element = tf.reshape(next, [-1,72])
state_batch = tf.squeeze(next_element)
dataset = dataset.prefetch(256)

# LAYERS

eps = 0.001
is_training = False
global_step = tf.Variable(0)
mean_update_rate = tf.train.exponential_decay(0.2, global_step, 500, 0.7)

if is_training:
    print "TRAINING MODE ON"
else:
    print "TRAINING MODE OFF"

# Input layer -- scale and recenter data.
with tf.name_scope('transform_in'):
    state_in = tf.placeholder_with_default(state_batch, shape=[None,72], name='transform_input')
    # with tf.device('/gpu:0'):
    mins_so_far = tf.Variable(initial_value=tf.zeros([1, 72], tf.float32),trainable=False)
    maxes_so_far = tf.Variable(initial_value=tf.zeros([1, 72], tf.float32),trainable=False)
    scaler_so_far = tf.Variable(initial_value=tf.zeros([1, 72], tf.float32),trainable=False)

    #total_ele_so_far = tf.Variable(initial_value=tf.zeros([], tf.int64))
    mean_so_far = tf.Variable(initial_value=tf.zeros([1,72], tf.float32),trainable=False)

    if is_training:
        mins_so_far = tf.assign(mins_so_far,tf.minimum(mins_so_far, tf.reduce_min(state_in, axis=0)))
        maxes_so_far = tf.assign(maxes_so_far,tf.maximum(mins_so_far, tf.reduce_max(state_in, axis=0)))
        scaler_so_far = tf.assign(scaler_so_far, tf.subtract(maxes_so_far, mins_so_far))
        this_mean = tf.reduce_mean(state_in, axis=0)

        mean_so_far = tf.assign(mean_so_far,
                                tf.add(tf.scalar_mul(mean_update_rate, this_mean),
                                       tf.scalar_mul(tf.subtract(tf.constant(1, dtype=tf.float32), mean_update_rate), mean_so_far)))

        # weighted_sum =
        # total_ele_so_far = tf.assign_add(total_ele_so_far,this_num_ele)


    scaled_state = tf.div(tf.subtract(state_in, mean_so_far), tf.add(scaler_so_far, eps))

# Encode the transformed input.
with tf.name_scope('encoder'):
    scaled_state_in = tf.placeholder_with_default(scaled_state, shape=[None, 72], name='encoder_input')
    layers = [72,58,48,32,28,18,10]
    out = sequential_layers(state_batch,layers, 'encode')

with tf.name_scope('decoder'):
    compressed_state = tf.placeholder_with_default(out, shape=[None,layers[-1]], name='decoder_input')
    decompressed_state = sequential_layers(compressed_state, list(reversed(layers)), 'decode')

with tf.name_scope('transform_out'):
     state_to_unscale= tf.placeholder_with_default(decompressed_state, shape=[None,72], name='transform_output')
     state_out = tf.add(tf.multiply(state_to_unscale,tf.add(scaler_so_far, eps)), mean_so_far)

with tf.name_scope('loss'):
    loss_op = tf.losses.absolute_difference(scaled_state_in, decompressed_state)
adam = tf.train.AdamOptimizer(learn_rate)
with tf.name_scope('training'):
    train_op = adam.minimize(loss_op, global_step=global_step, name="optimizer")

# FINAL SETUP

# Add ops to save and restore all the variables -- checkpoint style
saver = tf.train.Saver()

# Create a summary to monitor cost tensor
tf.summary.scalar("loss", loss_op)

# Merge all summaries into a single op
merged_summary_op = tf.summary.merge_all()

coord = tf.train.Coordinator() # Can kill everything when code is done. Prevents the `Skipping cancelled enqueue attempt with queue not closed'
np.set_printoptions(threshold=np.nan)

run_metadata = tf.RunMetadata()
run_options = tf.RunOptions(trace_level=tf.RunOptions.FULL_TRACE)

# Config to turn on JIT compilation
config = tf.ConfigProto()
config.gpu_options.force_gpu_compatible = True

# config.graph_options.optimizer_options.global_jit_level = tf.OptimizerOptions.ON_1

'''
EXECUTE NET
'''


with tf.Session(config=config) as sess:
    # Initialize all variables.
    sess.run(tf.global_variables_initializer())

    if os.path.isfile("./logs/model2.ckpt.meta"):
      saver.restore(sess, "./logs/model2.ckpt")
      print('restored')


    # Clear old log files.
    dir_name = "./logs"
    test = os.listdir(dir_name)
    for item in test:
        if item.endswith(".matt-desktop"):
            os.remove(os.path.join(dir_name, item))

    summary_writer = tf.summary.FileWriter("./logs", graph=tf.get_default_graph())

    # graph = tf.get_default_graph()
    # for i in tf.get_default_graph().get_operations():
    #     print i.name

    sess.run(iterator.initializer, feed_dict={filenames: filename_list})
    old_time = time.time()

    for i in range(100000000):
        if i%print_freq == 0:

            loss, _, summary, true_state, est_state, sca, me, decomp, ss = sess.run([loss_op, train_op, merged_summary_op, state_in, state_out, scaler_so_far, mean_so_far, decompressed_state, scaled_state], options=run_options,run_metadata = run_metadata) # est_state, true_state decompressed_state, full_state
            # loss, _, est_state, true_state = sess.run([loss_op, train_op, decompressed_state, full_state])
            #print np.shape(true_state)

            summary_writer.add_summary(summary, i)
            summary_writer.add_run_metadata(run_metadata, str(i))
            new_time = time.time()
            ips = batch_size*print_freq/(new_time - old_time)# options=tf.RunOptions(trace_level=tf.RunOptions.FULL_TRACE), run_metadata=run_metadata)

            print '\n' + '\033[1m'
            print("Iter: %d, Loss %f, runs/s %0.1f" % (i, loss, ips))
            print '\033[0m'
            save_path = saver.save(sess, "./logs/model2.ckpt")
            old_time = time.time() # Don't count the saving in our time estimate

            # st_diff = est_state - true_state
            #
            # print tabulate([['All x', np.mean(np.abs(st_diff[:,::6])), np.max(true_state[:,::6]), np.max(est_state[:,::6]), np.min(true_state[:,::6]), np.min(est_state[:,::6])],
            #                 ['All y', np.mean(np.abs(st_diff[:, 1::6])), np.max(true_state[:, 1::6]), np.max(est_state[:, 1::6]), np.min(true_state[:, 1::6]), np.min(est_state[:, 1::6])],
            #                 ['All th', np.mean(np.abs(st_diff[:, 2::6])), np.max(true_state[:, 2::6]), np.max(est_state[:, 2::6]), np.min(true_state[:, 2::6]), np.min(est_state[:, 2::6])],
            #                 ['All xd', np.mean(np.abs(st_diff[:, 3::6])), np.max(true_state[:, 3::6]), np.max(est_state[:, 3::6]), np.min(true_state[:, 3::6]), np.min(est_state[:, 3::6])],
            #                 ['All yd', np.mean(np.abs(st_diff[:, 4::6])), np.max(true_state[:, 4::6]), np.max(est_state[:, 4::6]), np.min(true_state[:, 4::6]), np.min(est_state[:, 4::6])],
            #                 ['All thd', np.mean(np.abs(st_diff[:, 5::6])), np.max(true_state[:, 5::6]), np.max(est_state[:, 5::6]), np.min(true_state[:, 5::6]), np.min(est_state[:, 5::6])]],
            #                headers=['Variable', 'MeanAbsErr', 'Max actual', 'max pred', 'Min actual', 'min pred'])
            #
            # np.save('est_st.npy', est_state)
            # np.save('tr_st.npy', true_state)
            # np.save('est_st_unsc.npy', decomp)
            # np.save('tr_st_unsc.npy', ss)

            # print str(sca) + "," + str(me)
            # fig1 = plt.figure()
            # ax1 = fig1.add_subplot(111)
            # ax1.plot(range(len(decomp[0:1000,0])),decomp[0:1000,12], '.b')
            # fig2 = plt.figure()
            # ax2 = fig2.add_subplot(111)
            # ax2.plot(range(len(ss[0:1000,0])),ss[0:1000,12], '.b')
            # plt.show()
        else:

            sess.run([train_op])


    # trace = timeline.Timeline(step_stats=run_metadata.step_stats)
    # trace_file = open('timeline.ctf.json', 'w') # View in chrome://tracing
    # trace_file.write(trace.generate_chrome_trace_format())

    # Stop all the queue threads.
    coord.request_stop()
    coord.join(threads)
