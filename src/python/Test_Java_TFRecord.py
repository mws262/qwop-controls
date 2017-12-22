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
    features = tf.parse_single_example(
        serialized_example,
        feature_lists={
            # We know the length of both fields. If not the
            # tf.VarLenFeature could be used
            'x': tf.FixedLenFeature([72], tf.float32),
            'y': tf.FixedLenFeature([1], tf.float32)
        })


    # now return the converted data
    x = features['x']
    y = features['y']
    return x, y





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

x_loaded, y_loaded = read_and_decode_single_example(['../../denseData_2017-11-06_08-57-41.NEWNEWNEW', '../../denseData_2017-11-06_08-57-41.NEWNEWNEW'])
print x_loaded
with tf.Session() as sess:
    # ... init our variables, ...
    sess.run(tf.global_variables_initializer())
    tf.train.start_queue_runners(sess=sess)
    x_input, y_input = sess.run([x_loaded, y_loaded])
    print(x_input)