import numpy as np
import densedata_pb2 as dataset
import tensorflow as tf
import time
import timeit
import os.path
import math

# Test and debug tfrecord files written from my java end.

def read_and_decode_single_example(filename):
    # first construct a queue containing a list of filenames.
    # this lets a user split up there dataset in multiple files to keep
    # size down
    filename_queue = tf.train.string_input_producer(filename,
                                                    num_epochs=None)
    # Unlike the TFRecordWriter, the TFRecordReader is symbolic
    reader = tf.TFRecordReader()
    # One can read a single serialized example from a filename
    # serialized_example is a Tensor of type string.
    _, serialized_example = reader.read(filename_queue)
    # The serialized example is converted back to actual values.
    # One needs to describe the format of the objects to be returned

    sequence_features = {
        'PRESSED_KEYS': tf.FixedLenSequenceFeature(shape=[], dtype=tf.string),
        'TIME_TO_TRANSITION': tf.FixedLenSequenceFeature(shape=[], dtype=tf.string),
        'ACTIONS': tf.FixedLenSequenceFeature(shape=[], dtype=tf.string)
    }

    features = tf.parse_single_sequence_example(serialized=serialized_example, sequence_features=sequence_features)

    # features = tf.parse_example(
    #     serialized_example,
    #     features={
    #         # We know the length of both fields. If not the
    #         # tf.VarLenFeature could be used
    #         'PRESSED_KEYS': tf.FixedLenSequenceFeature(tf.string),
    #         'TIME_TO_TRANSITION': tf.VarLenFeature(tf.string),
    #         #'ACTIONS': tf.FixedLenFeature([5], tf.string)
    #     })

    # now return the converted data
    print features
    #x = features[1]['PRESSED_KEYS'] # 1 means the sequence_features. 0 index would be context_features
    y = features[1]['TIME_TO_TRANSITION']
    print y
    return y





# filename_list = []
# print os.listdir('../../')
# for file in os.listdir('../../'):
#     if file.endswith('.NEWNEWNEW'):
#         filename_list.append(file)
#
# filename_list.append(filename_list)
#
# print filename_list
# print('%d files in queue.' % len(filename_list))

y_loaded = read_and_decode_single_example(['../../denseDataTest.NEWNEWNEW'])

y_loaded = tf.reshape(y_loaded,[1,1])
# groups examples into batches
y_batch = tf.train.batch(
    [y_loaded], batch_size=1)

with tf.Session() as sess:
    # ... init our variables, ...
   # print sess.run(x_loaded)
    sess.run(tf.global_variables_initializer())
    tf.train.start_queue_runners(sess=sess)
    y_input = sess.run(y_batch)
    print(y_input)