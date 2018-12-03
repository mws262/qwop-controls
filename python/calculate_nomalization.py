from PIL import Image

import tensorflow as tf
import numpy as np
import os.path

'''
Find the mean, min, max, range, stdev of a bunch of tfrecord state data. Save to file.
'''

'''
PARAMETERS & SETTINGS
'''

tfrecordExtension = '.TFRecord'  # File extension for input datafiles. Datafiles must be TFRecord-encoded protobuf format.
tfrecordPaths = ['../src/main/resources/saved_data/training_data/', '../src/main/resources/saved_data/11_2_18/']  # Location of datafiles on this machine. Beware of drive mounting locations.,

export_dir = './models/'
learn_rate = 1e-3

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

    return statesConcat

'''
DEFINE SPECIFIC DATAFLOW
'''

# Make a list of TFRecord files.
filename_list = []
for tfrecordPath in tfrecordPaths:
    for file in os.listdir(tfrecordPath):
        if file.endswith(tfrecordExtension):
            nextFile = tfrecordPath + file
            filename_list.append(nextFile)
            print(nextFile)

filenames = tf.placeholder(tf.string, shape=[None])
dataset = tf.data.TFRecordDataset(filenames)
dataset = dataset.map(_parse_function)
dataset = dataset.batch(1)
iterator = dataset.make_initializable_iterator()

next_element = iterator.get_next()


print('%d files in queue.' % len(filename_list))

do_calc = True
with tf.Session() as sess:
    sess.run(tf.global_variables_initializer())

    if do_calc:
        sess.run(iterator.initializer, feed_dict={filenames: filename_list})

        # Find mean of each state and min/max.
        state_sum = np.zeros(shape=[72])
        state_min = np.zeros(shape=[72]) + 1e12
        state_max = np.zeros(shape=[72]) - 1e12
        state_mean = np.zeros(shape=[72])
        state_range = np.zeros(shape=[72])
        state_stdev = np.zeros(shape=[72])
        state_count = 0
        try:
            while True:
                # try:
                next_state = sess.run([next_element])[0][0]
                # except:
                #     print("caught one")

                state_sum += np.sum(next_state, axis=0)
                state_count += np.shape(next_state)[0] - 1

                state_max = np.maximum(state_max, np.amax(next_state, axis=0))
                state_min = np.minimum(state_min, np.amin(next_state, axis=0))
        except tf.errors.OutOfRangeError:
            state_mean = np.divide(state_sum, state_count)
            state_range = state_max - state_min
            print(state_mean)
            print(state_min)
            print(state_max)
            pass

        # Find standard deviation of each state.
        sess.run(iterator.initializer, feed_dict={filenames: filename_list})
        stdev_sum = np.zeros(shape=[72])
        try:
            while True:
                next_state = sess.run([next_element])[0][0]
                next_state -= state_mean
                next_state = np.power(next_state,2)
                stdev_sum += np.sum(next_state, axis=0)
        except tf.errors.OutOfRangeError:
            state_stdev = np.sqrt(np.divide(stdev_sum, state_count - 1))
            print(state_stdev)
            pass

        file = open("saved_normalization.info", 'w')

        np.savez(file, mean=state_mean, max=state_max, min=state_min, range=state_range, std=state_stdev, quantity=state_count)
        file.close()

    # Test
    file = open("saved_normalization.info", 'r')
    npzfile = np.load(file)
    sess.run(iterator.initializer, feed_dict={filenames: filename_list})
    for i in range(1000):
        next_state = sess.run([next_element])[0][0]

        next_state = np.subtract(next_state, npzfile['min'])
        next_state = np.divide(next_state, npzfile['range'], out=np.zeros_like(next_state), where=npzfile['range']!=0)

        print(np.amin(np.amin(next_state, axis=0)))
        print(np.argmin(np.amin(next_state, axis=0)))
        print(np.amax(np.amax(next_state, axis=0)))
        print(np.argmax(np.amax(next_state, axis=0)))
        #print(next_state)
    file.close()



