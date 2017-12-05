import argparse
import tensorflow as tf


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
frozen_model_filename = "./tmp/frozen_model.pb"

# We use our "load_graph" function
graph = load_graph(frozen_model_filename)

# We can verify that we can access the list of operations in the graph
for op in graph.get_operations():
    print(op.name)

# We access the input and output nodes
x = graph.get_tensor_by_name('input/x-input:0')
y = graph.get_tensor_by_name('layer5/activation:0')
drp = graph.get_tensor_by_name('dropout/Placeholder:0')

# We launch a Session
with tf.Session(graph=graph) as sess:
    y_out = sess.run(y, feed_dict={
        x: [range(0,72)], drp: [1]})
    print(y_out)