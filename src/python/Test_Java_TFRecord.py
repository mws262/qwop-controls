import numpy as np
import densedata_pb2 as dataset
import tensorflow as tf
import time
import timeit
import os.path
import math

# Test and debug tfrecord files written from my java end.

feature = 'TIME_TO_TRANSITION'

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
    features = tf.parse_single_sequence_example(
        serialized=serialized_example,
        sequence_features={
            # We know the length of both fields. If not the
            # tf.VarLenFeature could be used
            'BODY': tf.FixedLenSequenceFeature([6],tf.float32,True),
            'PRESSED_KEYS': tf.FixedLenSequenceFeature([],tf.string,True),
            'TIME_TO_TRANSITION': tf.FixedLenSequenceFeature([],tf.string,True),
            'ACTIONS': tf.FixedLenSequenceFeature([],tf.string,True)
        })

    # now return the converted data
    body = features[1]['BODY']
    pk = tf.decode_raw(features[1]['PRESSED_KEYS'],tf.uint8)
    tt = tf.decode_raw(features[1]['TIME_TO_TRANSITION'],tf.uint8),
    act = tf.decode_raw(features[1]['ACTIONS'],tf.uint8)
    return pk,tt,act,body

filename_list = []
print os.listdir('../../')
for file in os.listdir('../../'):
    if file.endswith('.NEWNEWNEW'):
        filename_list.append(file)

filename_list.append(filename_list)

print filename_list
print('%d files in queue.' % len(filename_list))

options = tf.python_io.TFRecordOptions(tf.python_io.TFRecordCompressionType.NONE)


pk,tt,act,body = read_and_decode_single_example(['../../denseData_2017-11-06_08-57-41.NEWNEWNEW'])#'../../denseData_2017-11-06_08-57-41.NEWNEWNEW', '../../denseData_2017-11-06_08-57-41.NEWNEWNEW'])
with tf.Session() as sess:
    # ... init our variables, ...
    sess.run(tf.global_variables_initializer())
    tf.train.start_queue_runners(sess=sess)
    pkIn = sess.run([pk])
    ttIn = sess.run([tt])
    actIn = sess.run([act])
    bodyIn = sess.run([body])
    print pkIn
    print ttIn
    print actIn
    print bodyIn
    #
    # for i in range(10):
    #     ttIn = sess.run([tt])
    #     print ttIn
