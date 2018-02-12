import tensorflow as tf
import numpy as np

export_dir = './logs'
# loading
with tf.Session(graph=tf.Graph()) as sess:
    tf.saved_model.loader.load(sess, ["tag"], export_dir)
    graph = tf.get_default_graph()
    for i in tf.get_default_graph().get_operations():
        print i.name

    # x = graph.get_tensor_by_name("input/x-input:0")
    # dropout = graph.get_tensor_by_name("dropout/dropout_keep_probability:0")
    # model = graph.get_tensor_by_name("layer3/Wx_plus_b/pre_activations:0")
    # (sess.run(model, {x: np.reshape(np.arange(72,dtype=np.float32),[1,72]), dropout: 1}))