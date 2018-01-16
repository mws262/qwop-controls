import tensorflow as tf
import numpy as np
import os.path

'''
PARAMETERS & SETTINGS
'''

tfrecordExtension = '.NEWNEWNEW'  # File extension for input datafiles. Datafiles must be TFRecord-encoded protobuf format.
tfrecordPath = '../../'  # Location of datafiles on this machine.

export_dir = './models/'
learn_rate = 1e-4

initWeightsStdev = 0.1
initBiasVal = 0.1

# All states found in the TFRECORD files
stateKeys = ['BODY', 'HEAD', 'RTHIGH', 'LTHIGH', 'RCALF', 'LCALF',
             'RFOOT', 'LFOOT', 'RUARM', 'LUARM', 'RLARM', 'LLARM']

# Various action parameterizations put in the TFRECORD files
actionKeys = ['PRESSED_KEYS', 'TIME_TO_TRANSITION', 'ACTIONS']

# Tensorflow placeholders for for sequence and action features as defined by the stateKeys and actionKeys above.
sequence_features = {skey: tf.FixedLenSequenceFeature([6], tf.float32, True) for skey in stateKeys}
sequence_features.update({akey: tf.FixedLenSequenceFeature([], tf.string, True) for akey in actionKeys})


'''
FUNCTIONS IN THE NN PIPELINE
'''


def read_and_decode_example(filename):
    """
    Handles queueing and loading from TFRecord data files.

    :param filename: A list of filenames to load data from.
    :return: Dictionaries of tensors for state and various parameterizations of action.
    """

    # Construct a queue containing a list of filenames.
    filename_queue = tf.train.string_input_producer(filename, num_epochs=None)

    reader = tf.TFRecordReader()  # Reader is symbolic, unlike writer.

    # Read a single serialized example from a filename.
    _, serialized_example = reader.read(filename_queue)

    # The serialized example is converted back to actual values.
    features = tf.parse_single_sequence_example(
        serialized=serialized_example,
        sequence_features=sequence_features
    )
    states_in = {key: features[1][key] for key in stateKeys}
    actions_in= {key: tf.decode_raw(features[1][key], tf.uint8) for key in actionKeys}

    return states_in, actions_in


def process_state(state_dict):
    """
    Do any necessary post processing to the loaded states in order to make it feedable into the NN.

    :param state_dict: The dictionary of state variables immediately decoded from TFRecord files.
    :return: All the state variables concatenated into a single row.
    """
    return tf.reshape(tf.concat(values=state_dict.values(), axis=1),[-1,72])


def process_action(action_dict):
    """
    Do any necessary post processing to the loaded actions in order to make it feedable into the NN.

    :param action_dict: The dictionary of action parameterizations immediately decoded from TFRecord files.
    :return: Processed actions tensor.
    """
    print tf.reshape(action_dict[actionKeys[1]],[-1,1])
    return tf.reshape(action_dict[actionKeys[1]],[-1,1])  # For now, just time to transition. Others thrown away.

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

print('%d files in queue.' % len(filename_list))

# Read in data and rearrange.
states,actions = read_and_decode_example(filename_list)
states_processed = process_state(states)
actions_processed = process_action(actions)

# Groups examples into batches randomly
state_batch, action_batch = tf.train.shuffle_batch(
    [states_processed, actions_processed], batch_size=1000,
    capacity=20000,
    min_after_dequeue=500,
    enqueue_many=False, # I think this might be wrong.
    allow_smaller_final_batch=False,
    name='rand_batch')

# LAYERS

# Input layer.
with tf.name_scope('input'):
    state = tf.placeholder(tf.float32, shape=[None, 72], name='state-input')
    action_true = tf.placeholder(tf.int32, shape=[None, 1], name='action-input')

# Layer 1: Fully-connected.
layer1 = nn_layer(state, 72, 72*4, 'layer1')

# Layer 2: Fully connected, with dropout. During training, some nodes are trimmed out to keep the net general.
with tf.name_scope('dropout'):
    keep_prob = tf.placeholder(tf.float32)
    tf.summary.scalar('dropout_keep_probability', keep_prob)
    dropped = tf.nn.dropout(layer1, keep_prob)
    layer2 = nn_layer(dropped, 72*4, 72, 'layer2')

action_pred = nn_layer(layer2, 72, 1, 'layer3')

with tf.name_scope('Loss'):
    loss_op = tf.losses.mean_squared_error(action_true, action_pred)
    #loss_op = tf.reduce_mean(tf.divide(tf.square(tf.subtract(tf.cast(y_true, tf.float32),y)),tf.add(tf.cast(y_true, tf.float32),2.0)))
with tf.name_scope('Accuracy'):
    accuracy = tf.reduce_mean(tf.cast(tf.equal(tf.cast(tf.round(action_pred),tf.int32),tf.cast(action_true,tf.int32)),tf.float32))

# Training operation.
adam = tf.train.AdamOptimizer(learn_rate)
train_op = adam.minimize(loss_op, name="optimizer")


# FINAL SETUP

# Add ops to save and restore all the variables -- checkpoint style
saver = tf.train.Saver()

# Create a summary to monitor cost tensor
tf.summary.scalar("loss", loss_op)
tf.summary.scalar("accuracy", accuracy)

# Merge all summaries into a single op
merged_summary_op = tf.summary.merge_all()

'''
EXECUTE NET
'''

with tf.Session() as sess:
    # Initialize all variables.
    sess.run(tf.global_variables_initializer())
    # Ready data input queues.
    tf.train.start_queue_runners(sess=sess)

    # #if os.path.isfile("./tmp/model.ckpt"):
    #  saver.restore(sess, "./tmp/model.ckpt")
    # print('Loaded checkpoint file')
    # builder.add_meta_graph_and_variables(sess)
    # for n in tf.get_default_graph().as_graph_def().node:
    #     print(n.name)

    # Summary written to be used by TensorBoard
    summary_writer = tf.summary.FileWriter("./logs", graph=tf.get_default_graph())

    # TRAIN
    for i in range(100):
        state_input, action_input = sess.run([state_batch, action_batch])

        _, loss, acc, summary = sess.run([train_op, loss_op, accuracy, merged_summary_op],feed_dict={state: state_input, action_true: action_input, keep_prob: 0.9})

        summary_writer.add_summary(summary, i)

        print('iter:%d - loss:%f - accuracy:%f' % (i, loss, acc))
        # if i % 10 == 9:
        #     save_path = saver.save(sess, "./tmp/model.ckpt")







#     # now return the converted data
#     body = features[1]['BODY']
#     states = {key: features[1][key] for key in stateKeys}
#     pk = tf.decode_raw(features[1]['PRESSED_KEYS'],tf.uint8)
#     tt = tf.decode_raw(features[1]['TIME_TO_TRANSITION'],tf.uint8),
#     act = tf.decode_raw(features[1]['ACTIONS'],tf.uint8)
#     return pk,tt,act,body
#
#     'BODY': tf.FixedLenSequenceFeature([6], tf.float32, True),
#     'HEAD': tf.FixedLenSequenceFeature([6], tf.float32, True),
#     'RTHIGH': tf.FixedLenSequenceFeature([6], tf.float32, True),
#     'LTHIGH': tf.FixedLenSequenceFeature([6], tf.float32, True),
#     'RCALF': tf.FixedLenSequenceFeature([6], tf.float32, True),
#     'LCALF': tf.FixedLenSequenceFeature([6], tf.float32, True),
#     'RFOOT': tf.FixedLenSequenceFeature([6], tf.float32, True),
#     'LFOOT': tf.FixedLenSequenceFeature([6], tf.float32, True),
#     'RUARM': tf.FixedLenSequenceFeature([6], tf.float32, True),
#     'LUARM': tf.FixedLenSequenceFeature([6], tf.float32, True),
#     'RLARM': tf.FixedLenSequenceFeature([6], tf.float32, True),
#     'LLARM': tf.FixedLenSequenceFeature([6], tf.float32, True),
#     # Actions -- parameterized in several potentially useful ways
#     'PRESSED_KEYS': tf.FixedLenSequenceFeature([], tf.string, True),  # Just [1 0 0 1] for QP e.g.
#     'TIME_TO_TRANSITION': tf.FixedLenSequenceFeature([], tf.string, True),  # 5,4,3,2,1,16,15,14...
#     'ACTIONS': tf.FixedLenSequenceFeature([], tf.string, True)  # [5,1,0,0,1],[16,0,0,0,0], etc