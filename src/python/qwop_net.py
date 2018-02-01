import tensorflow as tf
import os.path
import random
from tensorflow.python.client import timeline
from tensorflow.python.ops import rnn, rnn_cell
import sys
import numpy as np

'''
PARAMETERS & SETTINGS
'''

tfrecordExtension = '.tfrecord'  # File extension for input datafiles. Datafiles must be TFRecord-encoded protobuf format.
tfrecordPath = '/mnt/QWOP_Tfrecord_1_20/'  # Location of datafiles on this machine. Beware of drive mounting locations.
# On external drive ^. use sudo mount /dev/sdb1 /mnt

export_dir = './models/'
learn_rate = 1e-3

initWeightsStdev = 0.1

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
random.shuffle(filename_list) # Shuffle so each time we restart, we get different order.

filenames = tf.placeholder(tf.string, shape=[None])
dataset = tf.data.TFRecordDataset(filenames)
dataset = dataset.map(_parse_function, num_parallel_calls=16)
#dataset = dataset.shuffle(buffer_size=5000)
dataset = dataset.repeat()  # Repeat the input indefinitely.
#dataset = dataset.batch(10)
dataset = dataset.padded_batch(200, padded_shapes=([2000,72]))
dataset = dataset.apply(tf.contrib.data.unbatch())
iterator = dataset.make_initializable_iterator()
next_element = iterator.get_next()
dataset = dataset.prefetch(1000)
state_batch = tf.squeeze(next_element)

# LAYERS

# Input layer.
with tf.name_scope('encoder'):
    full_state = tf.placeholder_with_default(state_batch, shape=[None,72], name='encoder_input')

    # Layer 1: Fully-connected.
    out = nn_layer(full_state, 72, 72, 'encode1')
    out = nn_layer(out, 72, 64, 'encode2')
    out = nn_layer(out, 64, 56, 'encode3')
    out = nn_layer(out, 56, 48, 'encode4')
    out = nn_layer(out, 48, 36, 'encode5')
    out = nn_layer(out, 36, 24, 'encode6')
    out = nn_layer(out, 24, 20, 'encode7')
    out = nn_layer(out, 20, 16, 'encode8')
    out = nn_layer(out, 16, 14, 'encode9')
    out = nn_layer(out, 14, 12, 'encode_out')

with tf.name_scope('decoder'):
    compressed_state = tf.placeholder_with_default(out, shape=[None,12], name='decoder_input')

    out = nn_layer(compressed_state, 12, 14, 'decode1')
    out = nn_layer(out, 14, 16, 'decode2')
    out = nn_layer(out, 16, 20, 'decode3')
    out = nn_layer(out, 20, 24, 'decode4')
    out = nn_layer(out, 24, 36, 'decode5')
    out = nn_layer(out, 36, 48, 'decode6')
    out = nn_layer(out, 48, 56, 'decode7')
    out = nn_layer(out, 56, 64, 'decode8')
    out = nn_layer(out, 64, 72, 'decode9')
    decompressed_state = nn_layer(out, 72, 72, 'decode_out')

with tf.name_scope('loss'):
    loss_op = tf.losses.mean_squared_error(full_state, decompressed_state)

with tf.name_scope('training'):
    # Training operation.
    adam = tf.train.AdamOptimizer(learn_rate)
    train_op = adam.minimize(loss_op, name="optimizer")

# FINAL SETUP

# Add ops to save and restore all the variables -- checkpoint style
saver = tf.train.Saver()

# Create a summary to monitor cost tensor
#tf.summary.scalar("loss", loss_op)

# Merge all summaries into a single op
#merged_summary_op = tf.summary.merge_all()

coord = tf.train.Coordinator() # Can kill everything when code is done. Prevents the `Skipping cancelled enqueue attempt with queue not closed'
np.set_printoptions(threshold=np.nan)
# run_metadata = tf.RunMetadata()

'''
EXECUTE NET
'''


with tf.Session() as sess:
    # Initialize all variables.
    sess.run(tf.global_variables_initializer())

    if os.path.isfile("./tmp/model2.ckpt.meta"):
      saver.restore(sess, "./tmp/model2.ckpt")
      print('restored')

    graph = tf.get_default_graph()
    for i in tf.get_default_graph().get_operations():
        print i.name

    print sess.run([compressed_state], feed_dict={full_state: np.resize(np.arange(72),[1,72])})

    threads = tf.train.start_queue_runners(sess=sess, coord=coord)
    sess.run(iterator.initializer, feed_dict={filenames: filename_list})

    for i in range(100000000):

        if i%499 == 0:
            loss, _, est_state, true_state = sess.run([loss_op, train_op, decompressed_state, full_state])
            print("Iter: %d, Loss %f" % (i,loss)) # options=tf.RunOptions(trace_level=tf.RunOptions.FULL_TRACE), run_metadata=run_metadata)
            save_path = saver.save(sess, "./tmp/model2.ckpt")

            st_diff = est_state - true_state
            error_percents = np.abs(np.divide(st_diff, true_state, out=np.zeros_like(st_diff), where=true_state != 0))
            avg_single_error = np.mean(error_percents[:,::6], axis=1)
            print(avg_single_error[::50])
            print('Total average error:')
            print(np.mean(avg_single_error))
        else:
            loss, _ = sess.run([loss_op, train_op])
            # for val in np.nditer(np.divide(reconstructed[-1,:] - state_next[-1,:],state_next[-1,:])):
            #     sys.stdout.write('%.2f' % val)
            #     sys.stdout.write(', ')
            # sys.stdout.write('\n')
            # for val in np.nditer(reconstructed[200,:]):
            #     sys.stdout.write('%2.2f' % val)
            #     sys.stdout.write(', ')
            # sys.stdout.write('\n')
            # for val in np.nditer(state_next[200,:]):
            #     sys.stdout.write('%2.2f' % val)
            #     sys.stdout.write(', ')
            # sys.stdout.write('\n')


    # trace = timeline.Timeline(step_stats=run_metadata.step_stats)
    # trace_file = open('timeline.ctf.json', 'w') # View in chrome://tracing
    # trace_file.write(trace.generate_chrome_trace_format())

    # Stop all the queue threads.
    coord.request_stop()
    coord.join(threads)
