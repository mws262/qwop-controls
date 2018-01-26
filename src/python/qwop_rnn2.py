import tensorflow as tf
import numpy as np
import os.path
from tensorflow.python.ops import rnn, rnn_cell

tfrecordExtension = '.tfrecord'  # File extension for input datafiles. Datafiles must be TFRecord-encoded protobuf format.
tfrecordPath = '/mnt/QWOP_Tfrecord_1_20/'  # Location of datafiles on this machine. Beware of drive mounting locations.

# All states found in the TFRECORD files
stateKeys = ['BODY', 'HEAD', 'RTHIGH', 'LTHIGH', 'RCALF', 'LCALF',
             'RFOOT', 'LFOOT', 'RUARM', 'LUARM', 'RLARM', 'LLARM']


sequence_features = {skey: tf.FixedLenSequenceFeature([6], tf.float32, True) for skey in stateKeys}
sequence_features.update({'PRESSED_KEYS': tf.FixedLenSequenceFeature([], tf.string, True)})
context_features = {'TIMESTEPS': tf.FixedLenFeature([],tf.int64,True)}

# Make a list of TFRecord files.
filename_list = []
for file in os.listdir(tfrecordPath):
    if file.endswith(tfrecordExtension):
        nextFile = tfrecordPath + file
        filename_list.append(nextFile)
        print(nextFile)

filename_queue = tf.train.string_input_producer(filename_list, num_epochs=None)
reader = tf.TFRecordReader()
_, serialized_example = reader.read(filename_queue)

# The serialized example is converted back to actual values.
context_read, sequence_read = tf.parse_single_sequence_example(
    serialized=serialized_example,
    context_features=context_features,
    sequence_features=sequence_features
)

timesteps = context_read['TIMESTEPS']
sequence_read['PRESSED_KEYS'] = tf.cast(
    tf.reshape(tf.decode_raw(sequence_read['PRESSED_KEYS'], tf.uint8), [-1, 4]), tf.float32)

statesConcat = tf.concat(sequence_read.values(), 1)


# Creating a new queue
padding_q = tf.PaddingFIFOQueue(
    capacity=10,
    dtypes=[tf.int64, tf.float32],
    shapes=[[],[None,76]])

# Enqueue the examples
enqueue_op = padding_q.enqueue([timesteps, statesConcat])

# Add the queue runner to the graph
qr = tf.train.QueueRunner(padding_q, [enqueue_op])
tf.train.add_queue_runner(qr)

# Dequeue padded data
batched_data = padding_q.dequeue_many(5)


cell = tf.nn.rnn_cell.BasicLSTMCell(num_units=64)

states_in = tf.placeholder(tf.float32, [5, None,76])
timesteps_in = tf.placeholder(tf.int64, [5])

outputs, last_states = tf.nn.dynamic_rnn(
    cell=cell,
    dtype=tf.float32,
    sequence_length=timesteps_in,
    inputs=states_in)



coord = tf.train.Coordinator() # Can kill everything when code is done. Prevents the `Skipping cancelled enqueue attempt with queue not closed'
with tf.Session() as sess:
    # Initialize all variables.
    sess.run(tf.global_variables_initializer())
    threads = tf.train.start_queue_runners(sess=sess, coord=coord)
    for i in range(100):
        lengths_batch, state_batch = sess.run(batched_data)
        out, lstates =  sess.run([outputs, last_states],feed_dict={timesteps_in: lengths_batch, states_in: state_batch})

        print np.shape(out)
        #print np.shape(lstates)