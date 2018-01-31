import tensorflow as tf
import numpy as np
import os.path
from tensorflow.python.ops import rnn, rnn_cell

tfrecordExtension = '.tfrecord'  # File extension for input datafiles. Datafiles must be TFRecord-encoded protobuf format.
tfrecordPath = '/mnt/QWOP_Tfrecord_1_20/'  # Location of datafiles on this machine. Beware of drive mounting locations.

# All states found in the TFRECORD files
stateKeys = ['BODY', 'HEAD', 'RTHIGH', 'LTHIGH', 'RCALF', 'LCALF',
             'RFOOT', 'LFOOT', 'RUARM', 'LUARM', 'RLARM', 'LLARM']

batch_size = 10


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
batched_data = padding_q.dequeue_many(batch_size)

states_in = tf.placeholder(tf.float32, [batch_size, None, 76])
timesteps_in = tf.placeholder(tf.int64, [batch_size])
max_ts = tf.placeholder(tf.int64, [])


def fully_connected(v_in, s_in, s_out, name, act=tf.nn.leaky_relu):
    with tf.variable_scope(name):
        W = tf.get_variable('W', initializer=tf.eye(num_rows=s_in, num_columns=s_out))
        b = tf.get_variable('b', [s_out], initializer=tf.constant_initializer(0.0))
        pre_activation = tf.reshape(
                tf.matmul(tf.reshape(v_in, [-1, s_in]), W) + b,
                tf.stack([batch_size, max_ts, s_out], axis=0))
        activated = act(pre_activation)
        return activated

def lstm(v_in, ts_in, s_in, name):
    with tf.variable_scope(name):
        cell = tf.nn.rnn_cell.BasicLSTMCell(num_units=s_in)
        rnn_outputs, last_states = tf.nn.dynamic_rnn(
            cell=cell,
            dtype=tf.float32,
            sequence_length=ts_in,
            inputs=v_in)
        return rnn_outputs, last_states


# activated = fully_connected(states_in, 76, 64, 'compressor1')
# activated = fully_connected(activated, 64, 48, 'compressor2')
# activated = fully_connected(activated, 48, 36, 'compressor3')
# activated = states_in
# rnn_outputs, _ = lstm(activated, timesteps_in, 76, 'lstm1')
# rnn_outputs, _ = lstm(rnn_outputs, timesteps_in, 76, 'lstm2')
# activated = rnn_outputs
# activated = fully_connected(rnn_outputs, 36, 48, 'decompressor1')
# activated = fully_connected(activated, 48, 64, 'decompressor2')
activated = fully_connected(states_in, 76, 72, 'decompressor3', act=tf.identity)

predictions = activated
# with tf.variable_scope('output_layer'):
#     size_in = 64
#     size_out = 72
#     W = tf.get_variable('W', initializer=tf.truncated_normal([size_in, size_out], stddev=1))
#     b = tf.get_variable('b', [size_out], initializer=tf.constant_initializer(0.1))
#     predictions = tf.reshape(
#             tf.matmul(tf.reshape(activated, [-1, size_in]), W) + b,
#             tf.stack([batch_size, max_ts, size_out], axis=0))

learning_rate = 1e-2
with tf.name_scope('Loss'):
    loss_op = tf.losses.mean_squared_error(predictions[:,:-1,:], states_in[:,1:,:-4])

#train_step = tf.train.AdagradOptimizer(learning_rate).minimize(loss_op)
adam = tf.train.AdamOptimizer(learning_rate)
train_op = adam.minimize(loss_op, name="optimizer")


coord = tf.train.Coordinator() # Can kill everything when code is done. Prevents the `Skipping cancelled enqueue attempt with queue not closed'
run_metadata = tf.RunMetadata()
with tf.Session() as sess:
    # Initialize all variables.
    sess.run(tf.global_variables_initializer())
    threads = tf.train.start_queue_runners(sess=sess, coord=coord)

    # lengths_batch, state_batch = sess.run(batched_data)
    # state_batch = state_batch[:,:100,:]
    # lengths_batch[0] = 100
    for i in range(1000):
        lengths_batch, state_batch = sess.run(batched_data)
        _, loss, pred =  sess.run([train_op, loss_op, predictions],
                                          feed_dict={timesteps_in: lengths_batch, states_in: state_batch, max_ts: np.amax(lengths_batch)}, options=tf.RunOptions(trace_level=tf.RunOptions.FULL_TRACE),
            run_metadata=run_metadata)
        if i%99 == 0:
            print loss
        #print pred[0, 1:11, 0:6] - state_batch[0, 0:10, 0:6]
        #print np.shape(lstates)

from tensorflow.python.client import timeline
trace = timeline.Timeline(step_stats=run_metadata.step_stats)
trace_file = open('timeline.ctf.json', 'w')
trace_file.write(trace.generate_chrome_trace_format())