import tensorflow as tf
import numpy as np

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


# Let's allow the user to pass the filename as an argument
frozen_model_filename = "./logs/frozen_model.pb"
# We use our "load_graph" function
graph = load_graph(frozen_model_filename)

# We can verify that we can access the list of operations in the graph
for op in graph.get_operations():
    print(op.name)

x = graph.get_tensor_by_name('tfrecord_input/Squeeze:0')
<<<<<<< HEAD
y = graph.get_tensor_by_name('encoder/encoder_output:0')


tmp1 = tf.multiply(y, 2)
with graph.as_default():
    a = tf.get_variable('a', shape=[1, 4], initializer=tf.random_normal_initializer(stddev=1e-1))


    b = tf.add(y,a)

=======
y = graph.get_tensor_by_name('decoder/decoder_input:0')

tmp1 = tf.multiply(y, 2)
>>>>>>> 2794723861571e4f486359db1b1398829d617b2a

state_next = np.array([ 2.02438141e+02, -7.76173949e-01, -8.75894010e-01,
         -6.09454393e-01,  5.14787865e+00, -3.25002372e-01,
          2.06726273e+02,  3.62310481e+00,  5.60029149e-01,
          5.73411608e+00, -6.03188419e+00, -6.39304519e-01,
          1.99600906e+02, -3.53730249e+00, -5.65013826e-01,
         -1.78917682e+00,  6.19298983e+00, -5.76285362e-01,
          2.03621552e+02, -2.21700311e+00,  1.13992274e-01,
         -2.42216051e-01, -1.56640217e-01, -2.27194834e+00,
          2.05932785e+02,  1.27989984e+00, -1.31003034e+00,
          1.61026692e+00, -1.50578380e+00, -3.01660228e+00,
          2.06234116e+02,  6.18872595e+00,  4.31489021e-01,
          7.36273003e+00, -5.66186190e+00,  9.03275430e-01,
          2.05772522e+02,  7.16886044e-01, -1.32795441e+00,
         -1.04135501e+00,  8.70878696e+00,  2.57445765e+00,
          2.11406418e+02,  4.00275278e+00, -3.13722581e-01,
         -2.47905064e+00,  1.45455561e+01, -7.29003012e-01,
          2.09011353e+02,  2.77249527e+00, -7.78181195e-01,
         -2.23896623e+00,  1.40690813e+01,  1.98155850e-01,
          2.03349304e+02, -7.32891083e-01, -2.40005755e+00,
         -4.29756254e-01,  1.16331158e+01,  2.15622616e+00,
          2.01113617e+02, -3.29934388e-01, -8.32069755e-01,
         -2.06226420e+00,  7.45629501e+00,  1.68408501e+00,
          2.02116318e+02, -1.81299829e+00, -1.03612328e+00,
          2.83644170e-01,  3.42957735e+00, -2.32843828e+00])

with tf.Session(graph=graph) as sess:
<<<<<<< HEAD
    sess.run(tf.global_variables_initializer())

    y_out, tmp1_out, b_out = sess.run([y, tmp1, b], feed_dict={
        x:[state_next]})
    print(y_out)
    print(tmp1_out)
    print(b_out)
=======
    y_out, tmp1_out = sess.run([y, tmp1], feed_dict={
        x:state_next})
    print(y_out)
    print(tmp1_out)
>>>>>>> 2794723861571e4f486359db1b1398829d617b2a



