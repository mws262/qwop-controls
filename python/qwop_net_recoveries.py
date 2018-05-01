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
## python freeze_checkpoint.py --model_dir "./logs" --output_node_names "softmax/Softmax"
## tensorboard --logdir=~/git/qwop_saveload/src/python/logs
tfrecordExtension = '.TFRecord'  # File extension for input datafiles. Datafiles must be TFRecord-encoded protobuf format.
tfrecordPath = '../saved_data/training_data/'  # Location of datafiles on this machine. Beware of drive mounting locations.
# On external drive ^. use sudo mount /dev/sdb1 /mnt OR /dev/sda2 for SSD

export_dir = './models/'
learn_rate = 1e-5

initWeightsStdev = 0.1

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
        x_out_list.append(tf.reshape(body_part[:,0] - xoffsets, [-1,1]))
        x_out_list.append(body_part[:,1:])

    statesConcat = tf.concat(x_out_list, 1, name='concat_states')

    #pk = tf.cast(tf.reshape(tf.decode_raw(features[1]['PRESSED_KEYS'], tf.uint8), [-1, 4]), tf.float32)
    # ttt = {'TIME_TO_TRANSITION': tf.reshape(tf.decode_raw(features[1]['TIME_TO_TRANSITION'], tf.uint8),)}
    # act = {'ACTIONS': tf.reshape(tf.decode_raw(features[1]['ACTIONS'], tf.uint8),(-1, 5))}

    pkoh = tf.cast(tf.reshape(tf.decode_raw(features[1]['PRESSED_KEYS_ONE_HOT'], tf.uint8), [-1, 3]), dtype=tf.float32)

    extended_states = tf.concat(values=[statesConcat, pkoh], axis=1) # States with actions attached.

    return extended_states


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
batch_size = 1
print_freq = 1999

# Make a list of TFRecord files.
filename_list = [] # tfrecordPath+'denseTF_2018-05-01_08-37-16.TFRecord', tfrecordPath+'denseTF_2018-05-01_08-38-39.TFRecord']
for file in os.listdir(tfrecordPath):
    if file.endswith(tfrecordExtension):
        nextFile = tfrecordPath + file
        filename_list.append(nextFile)
        print(nextFile)
random.shuffle(filename_list) # Shuffle so each time we restart, we get different order.

global_step = tf.Variable(0)

with tf.name_scope("tfrecord_input"):
    filenames = tf.placeholder(tf.string, shape=[None])
    dataset = tf.data.TFRecordDataset(filenames)
    dataset = dataset.map(_parse_function, num_parallel_calls=30)
    dataset = dataset.shuffle(buffer_size=50000)
    dataset = dataset.repeat()
    dataset = dataset.apply(tf.contrib.data.unbatch())
    dataset = dataset.batch(1000)
    #dataset = dataset.padded_batch(batch_size, padded_shapes=([None,72])) # Pad to max-length sequence
    iterator = dataset.make_initializable_iterator()
    next = iterator.get_next()
    next_element = tf.squeeze(tf.reshape(next, [-1, 75]))
    state_batch, keys = tf.split(next_element, [72,3], axis=1)
    dataset = dataset.prefetch(256)

# LAYERS
# Encode the transformed input.
with tf.name_scope('fully_connected'):
    scaled_state_in = tf.placeholder_with_default(state_batch, shape=[None, 72], name='fully_connected_input')
    layers = [72,30,10,3]
    out = sequential_layers(state_batch,layers, 'fully_connected')
    fully_connected_out = tf.identity(out, name='fully_connected_out') # Solely to make a convenient output to reference in the saved graph.
    mean_encodings = tf.reduce_mean(out, axis=0)
    for i in range(layers[-1]):
        tf.summary.scalar('encoding' + str(i), mean_encodings[i], family='encodings')

with tf.name_scope('softmax'):
    softmax_out = tf.nn.softmax(fully_connected_out)

with tf.name_scope('loss'):
    loss_op = tf.nn.softmax_cross_entropy_with_logits_v2(logits=fully_connected_out, labels=keys)
    reducedLoss = tf.reduce_mean(loss_op)
   # loss_op = tf.losses.absolute_difference(softmax_out, keys)

with tf.name_scope('training'):
    adam = tf.train.AdamOptimizer(learn_rate)
    train_op = adam.minimize(loss_op, global_step=global_step, name='optimizer')


# FINAL SETUP

# Add ops to save and restore all the variables -- checkpoint style
saver = tf.train.Saver()

# Create a summary to monitor cost tensor
tf.summary.scalar('loss', loss_op)

# Merge all summaries into a single op
merged_summary_op = tf.summary.merge_all()

coord = tf.train.Coordinator() # Can kill everything when code is done. Prevents the `Skipping cancelled enqueue attempt with queue not closed'
np.set_printoptions(threshold=np.nan)

#run_metadata = tf.RunMetadata()
#un_options = tf.RunOptions(trace_level=tf.RunOptions.FULL_TRACE)

# Config to turn on JIT compilation
config = tf.ConfigProto()
config.gpu_options.force_gpu_compatible = True
config.graph_options.optimizer_options.global_jit_level = tf.OptimizerOptions.ON_1


'''
EXECUTE NET
'''

with tf.Session(config=config) as sess:
    # Initialize all variables.
    sess.run(tf.global_variables_initializer())

    if os.path.isfile("./logs/checkpoint"):
      ckpt = tf.train.get_checkpoint_state("./logs")
      saver.restore(sess, ckpt.model_checkpoint_path)
      print('restored')
    #
    #
    # # Clear old log files.
    # dir_name = "./logs"
    # test = os.listdir(dir_name)
    # for item in test:
    #     if item.endswith(".matt-desktop"):
    #         os.remove(os.path.join(dir_name, item))

   #summary_writer = tf.summary.FileWriter("./logs", graph=tf.get_default_graph())

    sess.run(iterator.initializer, feed_dict={filenames: filename_list})
    old_time = time.time()
    for i in range(100000000):

        if i%print_freq == 0:
            loss, _, loss_mean = sess.run([loss_op, train_op, reducedLoss])#, options=run_options,run_metadata = run_metadata)
           # print sess.run(softmax_out)
            # print loss
            #summary_writer.add_summary(summary, i)
            #summary_writer.add_run_metadata(run_metadata, str(i))
            new_time = time.time()
            ips = batch_size*print_freq/(new_time - old_time)

            print '\n' + '\033[1m'
            print("Iter: %d, Loss %f, runs/s %0.1f" % (i, loss_mean, ips))
            print '\033[0m'
            save_path = saver.save(sess, "./logs/model2.ckpt", global_step=i)
            old_time = time.time() # Don't count the saving in our time estimate
        else:
            sess.run([train_op])

    # trace = timeline.Timeline(step_stats=run_metadata.step_stats)
    # trace_file = open('timeline.ctf.json', 'w') # View in chrome://tracing
    # trace_file.write(trace.generate_chrome_trace_format())
