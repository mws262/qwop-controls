import tensorflow as tf
import numpy as np

""" SETTINGS AND PARAMS """
export_dir = './models/'
learn_rate = 1e-4



stateKeys = ['BODY','HEAD','RTHIGH','LTHIGH','RCALF','LCALF','RFOOT','LFOOT','RUARM','LUARM','RLARM','LLARM']
actionKeys = ['PRESSED_KEYS','TIME_TO_TRANSITION','ACTIONS']
sequence_features = {skey: tf.FixedLenSequenceFeature([6],tf.float32,True) for skey in stateKeys}
sequence_features.update({akey: tf.FixedLenSequenceFeature([],tf.string,True) for akey in actionKeys})


def read_and_decode_single_example(filename):
    # first construct a queue containing a list of filenames.
    # this lets a user split up there dataset in multiple files to keep
    # size down
    filename_queue = tf.train.string_input_producer(filename, num_epochs=None)
    # Unlike the TFRecordWriter, the TFRecordReader is symbolic
    reader = tf.TFRecordReader()
    # One can read a single serialized example from a filename
    # serialized_example is a Tensor of type string.
    _, serialized_example = reader.read(filename_queue)
    # The serialized example is converted back to actual values.
    # One needs to describe the format of the objects to be returned
    features = tf.parse_single_sequence_example(
        serialized=serialized_example,
        sequence_features=sequence_features
    )
    states = {key : features[1][key] for key in stateKeys}
    actionsEncoded = {key: features[1][key] for key in actionKeys}
    actionsDecoded= {key : tf.decode_raw(features[1][key],tf.uint8) for key in actionKeys}

    return states, actionsDecoded

def arrange_state(stateDict):
    return tf.concat(values=stateDict.values(),axis=1)

states,actions = read_and_decode_single_example(['../../denseDataTest.NEWNEWNEW'])
statesTogether = arrange_state(states)






with tf.Session() as sess:
    # ... init our variables, ...
    sess.run(tf.global_variables_initializer())
    tf.train.start_queue_runners(sess=sess)
    s = sess.run(statesTogether)
    print np.shape(s)
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