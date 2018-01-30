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
    capacity=1000,
    dtypes=[tf.int64, tf.float32],
    shapes=[[],[None,76]])

# Enqueue the examples
enqueue_op = padding_q.enqueue([timesteps, statesConcat])

# Add the queue runner to the graph
qr = tf.train.QueueRunner(padding_q, [enqueue_op])
tf.train.add_queue_runner(qr)

# Dequeue padded data
batched_data = padding_q.dequeue_many(5)


cell = tf.nn.rnn_cell.BasicRNNCell(num_units=76)

states_in = tf.placeholder(tf.float32, [5, None, 76])
timesteps_in = tf.placeholder(tf.int64, [5])
max_ts = tf.placeholder(tf.int64, [])

rnn_outputs, last_states = tf.nn.dynamic_rnn(
    cell=cell,
    dtype=tf.float32,
    sequence_length=timesteps_in,
    inputs=states_in)


with tf.variable_scope('output_layer'):
    W = tf.get_variable('W', [76, 72])
    b = tf.get_variable('b', [72], initializer=tf.constant_initializer(0.1))
    predictions = tf.reshape(
            tf.matmul(tf.reshape(rnn_outputs, [-1, 76]), W) + b,
            tf.stack([5, max_ts, 72], axis=0))

learning_rate = 1e-2
with tf.name_scope('Loss'):
    loss_op = tf.losses.mean_squared_error(predictions[:,:-1,:], states_in[:,1:,:-4])

#train_step = tf.train.AdagradOptimizer(learning_rate).minimize(loss_op)
adam = tf.train.AdamOptimizer(learning_rate)
train_op = adam.minimize(loss_op, name="optimizer")


coord = tf.train.Coordinator() # Can kill everything when code is done. Prevents the `Skipping cancelled enqueue attempt with queue not closed'
with tf.Session() as sess:
    # Initialize all variables.
    sess.run(tf.global_variables_initializer())
    threads = tf.train.start_queue_runners(sess=sess, coord=coord)

    lengths_batch, state_batch = sess.run(batched_data)
    for i in range(10000):

        _, loss, st_out, pred =  sess.run([train_op, loss_op, rnn_outputs, predictions],feed_dict={timesteps_in: lengths_batch, states_in: state_batch, max_ts: np.amax(lengths_batch)})
        print loss
        print pred[0, 0:10, 0:6] - state_batch[0, 0:10, 0:6]
        #print np.shape(lstates)