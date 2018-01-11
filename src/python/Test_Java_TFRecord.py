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
        features={
            # We know the length of both fields. If not the
            # tf.VarLenFeature could be used
            'x': tf.VarLenFeature(tf.float32)
        })


    # now return the converted data
    x = features['x']
    return x

   # example proto decode
def _parse_function(example_proto):
    keys_to_features = {'x':tf.FixedLenFeature((1), tf.float32)}
    parsed_features = tf.parse_single_example(example_proto, keys_to_features)
    return parsed_features['x']

filenames = ['../../test.NEWNEWNEW']
dataset = tf.contrib.data.TFRecordDataset(filenames)

# Parse the record into tensors.
dataset = dataset.map(_parse_function)

# Shuffle the dataset
dataset = dataset.shuffle(buffer_size=100)

# Repeat the input indefinitly
dataset = dataset.repeat()

# Generate batches
dataset = dataset.batch(1)

# Create a one-shot iterator
iterator = dataset.make_one_shot_iterator()

# Get batch X and y
x = iterator.get_next()
print x

with tf.Session() as sess:
    sess.run(tf.global_variables_initializer())
    val = sess.run(x)
    print val
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
#
# options = tf.python_io.TFRecordOptions(tf.python_io.TFRecordCompressionType.NONE)
#
#
# x_loaded = read_and_decode_single_example(['../../test.NEWNEWNEW']);#'../../denseData_2017-11-06_08-57-41.NEWNEWNEW', '../../denseData_2017-11-06_08-57-41.NEWNEWNEW'])
# print x_loaded
# with tf.Session() as sess:
#     # ... init our variables, ...
#     sess.run(tf.global_variables_initializer())
#     tf.train.start_queue_runners(sess=sess)
#     x_input = sess.run([x_loaded])
#     print(x_input)