import argparse
import tensorflow as tf
import numpy as np
import os.path
import random


# All states found in the TFRECORD files
stateKeys = ['BODY', 'HEAD', 'RTHIGH', 'LTHIGH', 'RCALF', 'LCALF',
             'RFOOT', 'LFOOT', 'RUARM', 'LUARM', 'RLARM', 'LLARM']

# Various action parameterizations put in the TFRECORD files
actionKeys = ['PRESSED_KEYS', 'TIME_TO_TRANSITION', 'ACTIONS']

# Various information pertaining to the ENTIRE run, but not recorded at every timestep
contextKeys = ['TIMESTEPS']
# Tensorflow placeholders for for sequence and action features as defined by the stateKeys and actionKeys above.
sequence_features = {skey: tf.FixedLenSequenceFeature([6], tf.float32, True) for skey in stateKeys}
sequence_features.update({akey: tf.FixedLenSequenceFeature([], tf.string, True) for akey in actionKeys})
context_features = {ckey: tf.FixedLenFeature([],tf.int64,True) for ckey in contextKeys}
tfrecordExtension = '.tfrecord'  # File extension for input datafiles. Datafiles must be TFRecord-encoded protobuf format.
tfrecordPath = '/mnt/QWOP_Tfrecord_1_20/'  # Location of datafiles on this machine. Beware of drive mounting locations.



def load_graph(frozen_graph_filename):
    # We load the protobuf file from the disk and parse it to retrieve the
    # unserialized graph_def
    with tf.gfile.GFile(frozen_graph_filename, "rb") as f:
        graph_def = tf.GraphDef()
        graph_def.ParseFromString(f.read())

    # Then, we import the graph_def into a new Graph and returns it
    with tf.Graph().as_default() as graph:
        # The name var will prefix every op/nodes in your graph
        # Since we load everything in a new graph, this is not needed
        tf.import_graph_def(graph_def, name="")
    return graph

def _parse_function(example_proto):
    # The serialized example is converted back to actual values.
    features = tf.parse_single_sequence_example(
        serialized=example_proto,
        context_features=context_features,
        sequence_features=sequence_features,
        name='parse_sequence'
    )
    context = features[0]  # Total number of timesteps in here with key 'TIMESTEPS'
    ts = context['TIMESTEPS']

    # Subtract out the x component of the body from all other x components.
    xoffsets = features[1]['BODY'][:,0] # Get first column.
    x_out_list = []
    for key in stateKeys:
        body_part = features[1][key]
        x_out_list.append(tf.reshape(body_part[:,0] - xoffsets,[-1,1]))
        x_out_list.append(body_part[:,1:])

    #feats = {key: features[1][key] for key in stateKeys}  # States
    states_concat = tf.concat(x_out_list, 1, name='concat_states')

    pressed_keys = tf.reshape(tf.cast(tf.decode_raw(features[1]['PRESSED_KEYS'], tf.uint8), dtype=tf.float32), [-1, 4])
    extended_states = (states_concat, pressed_keys, ts)

    #tf.concat([states_concat, pressed_keys], 1, name='concat_actions')

    # ttt = {'TIME_TO_TRANSITION': tf.reshape(tf.decode_raw(features[1]['TIME_TO_TRANSITION'], tf.uint8),)}
    # act = {'ACTIONS': tf.reshape(tf.decode_raw(features[1]['ACTIONS'], tf.uint8),(5,))}

    # feats.update({key: tf.reshape(tf.decode_raw(features[1][key], tf.uint8),(1,)) for key in actionKeys})  # Attach game.actions too after decoding.
    #feats.update(pk)
    return extended_states


# Let's allow the user to pass the filename as an argument
frozen_model_filename = "./logs/frozen_model.pb"
# We use our "load_graph" function
graph = load_graph(frozen_model_filename)

# We can verify that we can access the list of operations in the graph
for op in graph.get_operations():
    print(op.name)

# We access the input and output nodes
trans_in = graph.get_tensor_by_name('import/transform/transform_input:0')
enc_out = graph.get_tensor_by_name('import/encoder/encoder_output:0')
rnn_key_in = graph.get_tensor_by_name('LSTM/key_command_input:0')
rnn_state_in = graph.get_tensor_by_name('LSTM/encoded_state:0')
rnn_ts_in = graph.get_tensor_by_name('LSTM/sequence_timesteps:0')
rnn_out = graph.get_tensor_by_name('LSTM/rnn_output:0')
dec_in = graph.get_tensor_by_name('import/decoder/decoder_input:0')
trans_out = graph.get_tensor_by_name('import/untransform/untransform_output:0')


with tf.name_scope("dataset_input"):
    with graph.as_default():
        filenames = tf.placeholder(tf.string, shape=[None])
        dataset = tf.data.TFRecordDataset(filenames)
        dataset = dataset.map(_parse_function, num_parallel_calls=16)
        # dataset = dataset.shuffle(buffer_size=5000)
        dataset = dataset.repeat()
        # dataset = dataset.padded_batch(batch_size, padded_shapes=([None,72])) # Pad to max-length sequence
        iterator = dataset.make_initializable_iterator()
        next = iterator.get_next()
        state_batch = next[0]
        keys_batch = next[1]
        timesteps_batch = next[2]
        dataset = dataset.prefetch(256)

filename_list = []
for file in os.listdir(tfrecordPath):
    if file.endswith(tfrecordExtension):
        nextFile = tfrecordPath + file
        filename_list.append(nextFile)
        #print(nextFile)
random.shuffle(filename_list) # Shuffle so each time we restart, we get different order.


def predict_next(sess, states, keys):
    encoded_state = sess.run(enc_out, feed_dict={trans_in: st[0:5,:]})
    lstm_out = sess.run(rnn_out, feed_dict={rnn_state_in: encoded_state, rnn_key_in: k[0:5,:], rnn_ts_in: 5})
    result_state = sess.run(trans_out, feed_dict={dec_in: lstm_out})
    return result_state

#state_next = np.reshape(state_next,[1,72])
with tf.Session(graph=graph) as sess:

    sess.run(iterator.initializer, feed_dict={filenames: filename_list})

    st, k, ts = sess.run([state_batch, keys_batch, timesteps_batch])  # Get a batch from tfrecords


    nextSt = predict_next(sess, st, k)

    for i in range(10):
        nextSt = predict_next(sess, nextSt, k)
        print nextSt[:,0:5]

# python freeze_checkpoint.py --model_dir "./logs" --output_node_names "LSTM/fully_connected_rnn_reshaper/activations,import/decoder/decoder_output"




