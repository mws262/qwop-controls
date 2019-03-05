import tensorflow as tf
import numpy as np
import os.path
from tensorflow.python.ops import rnn
import matplotlib.pyplot as plt
from random import shuffle

'''
PARAMETERS & SETTINGS
'''

tfrecordExtension = '.TFRecord'  # File extension for input datafiles. Datafiles must be TFRecord-encoded protobuf format.
tfrecordPaths = ['../src/main/resources/saved_data/training_data/']  # Location of datafiles on this machine. Beware of drive mounting locations.

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


'''
DEFINE SPECIFIC DATAFLOW
'''

# Make a list of TFRecord files.
filename_list = []

for tfrecordPath in tfrecordPaths:
    for avg_file in os.listdir(tfrecordPath):
        if avg_file.endswith(tfrecordExtension):
            nextFile = tfrecordPath + avg_file
            filename_list.append(nextFile)
            print(nextFile)


batch_size = 1

filenames = tf.placeholder(tf.string, shape=[None])
dataset = tf.data.TFRecordDataset(filenames)
dataset = dataset.map(_parse_function, num_parallel_calls=30)
dataset = dataset.shuffle(buffer_size=2000)
# dataset = dataset.repeat()  # Repeat the input indefinitely.
dataset = dataset.padded_batch(batch_size, padded_shapes=((), (None, 72), (None, 3)))
iterator = dataset.make_initializable_iterator()

next_element = iterator.get_next()

print('%d files in queue.' % len(filename_list))

# LAYERS
global_step = tf.Variable(0)

# Input layer.
with tf.name_scope('input'):
    sequence_length = tf.placeholder(tf.int32, shape=[None], name='run-timestep-count')
    qwop_state = tf.placeholder(tf.float32, shape=[None, None, 72], name='qwop_state_input')

    qwop_action = tf.placeholder(tf.float32, shape=[None, None, 3], name='qwop_action_input')
    extended_state = tf.concat([qwop_state, qwop_action], axis=2, name='concat_state_action')

'''
EXECUTE NET
'''
with tf.Session() as sess:
    # Initialize all variables.
    sess.run(tf.global_variables_initializer())

    sess.run(iterator.initializer, feed_dict={filenames: filename_list})

    st_all = np.zeros((1, 72))
    act_all = np.zeros((1, 3))
    # Do some testing and plotting rather than training.
    for i in range(100):
        try:
            state_next = sess.run([next_element])

            # st_in = np.expand_dims(state_next[0][1][0][0], axis=0)
            # st_in = np.expand_dims(st_in, axis=0)
            #
            # act_in = np.expand_dims(state_next[0][2][0][0], axis=0)
            # act_in = np.expand_dims(act_in, axis=0)

            st_in = state_next[0][1][0][:]
            act_in = state_next[0][2][0][:]
            st_all = np.vstack((st_all, st_in))
            act_all = np.vstack((act_all, act_in))
        except tf.errors.OutOfRangeError:
            break

    print('done collecting')
    # plot_end = int(state_next[0][0])
    # plt.subplot(2, 1, 1)
    # plt.plot(range(plot_end), pred_sim[0:plot_end, 1:6])

    plt.axis('off')
    fig, ax = plt.subplots(10, 72, sharex=True, sharey=True)
    fig.dpi = 100
    fig.set_size_inches(25, 10)

    for i in range(72):
        for j in range(i + 1, 72):
            # # Turn off tick labels
            # ax[i,j].set_yticklabels([])
            # ax[i,j].set_xticklabels([])

            ax[i, j].plot(st_all[act_all[:, 0] == 1, i], st_all[act_all[:, 0] == 1, j], 'r.', markersize=1)

            # plt.subplot(3, 1, 2)
            ax[i, j].plot(st_all[act_all[:, 1] == 1, i], st_all[act_all[:, 1] == 1, j], 'g.', markersize=1)

            # plt.subplot(3, 1, 3)
            ax[i, j].plot(st_all[act_all[:, 2] == 1, i], st_all[act_all[:, 2] == 1, j], 'b.', markersize=1)
            plt.show()
            if i * j > 10:
                break



    # plt.figure(1)
    # plt.plot(st_all[act_all[:, 0] == 1, 2], st_all[act_all[:, 0] == 1, 5], 'r.', markersize=1)
    #
    # # plt.subplot(3, 1, 2)
    # plt.plot(st_all[act_all[:, 1] == 1, 2], st_all[act_all[:, 1] == 1, 5], 'g.', markersize=1)
    #
    # # plt.subplot(3, 1, 3)
    # plt.plot(st_all[act_all[:, 2] == 1, 2], st_all[act_all[:, 2] == 1, 5], 'b.', markersize=1)
    # plt.show()
    #
    # plt.figure(2)
    # plt.plot(st_all[act_all[:, 0] == 1, 13], st_all[act_all[:, 0] == 1, 14], 'r.', markersize=1)
    #
    # # plt.subplot(3, 1, 2)
    # plt.plot(st_all[act_all[:, 1] == 1, 13], st_all[act_all[:, 1] == 1, 14], 'g.', markersize=1)
    #
    # # plt.subplot(3, 1, 3)
    # plt.plot(st_all[act_all[:, 2] == 1, 13], st_all[act_all[:, 2] == 1, 14], 'b.', markersize=1)
    # plt.show()