import tensorflow as tf
import numpy as np
import os.path
from tensorflow.python.ops import rnn, rnn_cell
import sys

'''
PARAMETERS & SETTINGS
'''

tfrecordExtension = '.tfrecord'  # File extension for input datafiles. Datafiles must be TFRecord-encoded protobuf format.
tfrecordPath = '/mnt/QWOP_Tfrecord_1_20/'  # Location of datafiles on this machine. Beware of drive mounting locations.
# On external drive ^. use sudo mount /dev/sdb1 /mnt

export_dir = './models/'
learn_rate = 1e-6

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
        sequence_features=sequence_features
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
    statesConcat = tf.concat(x_out_list,1)

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

# OVERRIDE:
# filename_list = ['../../denseData_2017-11-06_08-57-41.NEWNEWNEW']

filenames = tf.placeholder(tf.string, shape=[None])
dataset = tf.data.TFRecordDataset(filenames)
dataset = dataset.map(_parse_function)
#dataset = dataset.shuffle(buffer_size=5000)
dataset = dataset.repeat()  # Repeat the input indefinitely.
dataset = dataset.batch(1)  # .padded_batch(4, padded_shapes=[None])
iterator = dataset.make_initializable_iterator()

next_element = iterator.get_next()

# print('%d files in queue.' % len(filename_list))


# LAYERS

# Input layer.
with tf.name_scope('input'):
    state = tf.placeholder(tf.float32, shape=[None,72], name='state-input')
    #action_true = tf.placeholder(tf.int32, shape=[None, 1], name='action-input')


# Layer 1: Fully-connected.
out = nn_layer(state, 72, 64, 'layer1')
out = nn_layer(out, 64, 42, 'layer2')
out = nn_layer(out, 42, 25, 'layer2')
out = nn_layer(out, 25, 12, 'layer3')

out = nn_layer(out, 12, 25, 'layer4')
out = nn_layer(out, 25, 42, 'layer5')
out = nn_layer(out, 42, 64, 'layer2')
decompressed = nn_layer(out, 64, 72, 'layer6')

with tf.name_scope('Loss'):
    loss_op = tf.losses.mean_squared_error(state, decompressed)

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

    if os.path.isfile("./tmp/model1.ckpt.meta"):
      saver.restore(sess, "./tmp/model1.ckpt")
      print('restored')

    threads = tf.train.start_queue_runners(sess=sess, coord=coord)

    sess.run(iterator.initializer, feed_dict={filenames: filename_list})

    for i in range(10000000):
        state_next = np.squeeze(sess.run([next_element]))
        loss, _, reconstructed = sess.run([loss_op,train_op,decompressed], feed_dict={state: state_next})
        if i%99 == 0:
            print("Iter: %d, Loss %f" % (i,loss))
            save_path = saver.save(sess, "./tmp/model1.ckpt")
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

    # Stop all the queue threads.
    coord.request_stop()
    coord.join(threads)
