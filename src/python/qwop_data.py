import numpy as np
import densedata_pb2 as dataset
import tensorflow as tf
import time
import timeit
import os.path
import math

## tensorboard --logdir=~/git/qwop_saveload/src/python/logs
## First: source ~/tensorflow/bin/activate

export_dir = './models/'

learn_rate = 1e-4

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
            'x': tf.FixedLenFeature([72], tf.float32),
            'y': tf.FixedLenFeature([1], tf.float32)
        })
    # now return the converted data
    x = features['x']
    y = features['y']
    return x, y

def weight_variable(shape):
    """Create a weight variable with appropriate initialization."""
    initial = tf.truncated_normal(shape, stddev=0.1)
    return tf.Variable(initial)

def bias_variable(shape):
    """Create a bias variable with appropriate initialization."""
    initial = tf.constant(0.1, shape=shape)
    return tf.Variable(initial)

def variable_summaries(var):
  """Attach a lot of summaries to a Tensor (for TensorBoard visualization)."""
  with tf.name_scope('summaries'):
    mean = tf.reduce_mean(var)
    tf.summary.scalar('mean', mean)
    with tf.name_scope('stddev'):
      stddev = tf.sqrt(tf.reduce_mean(tf.square(var - mean)))
    tf.summary.scalar('stddev', stddev)
    tf.summary.scalar('max', tf.reduce_max(var))
    tf.summary.scalar('min', tf.reduce_min(var))
    tf.summary.histogram('histogram', var)

def nn_layer(input_tensor, input_dim, output_dim, layer_name, act=tf.nn.relu):
  """Reusable code for making a simple neural net layer.

  It does a matrix multiply, bias add, and then uses relu to nonlinearize.
  It also sets up name scoping so that the resultant graph is easy to read,
  and adds a number of summary ops.
  """
  # Adding a name scope ensures logical grouping of the layers in the graph.
  with tf.name_scope(layer_name):
    # This Variable will hold the state of the weights for the layer
    with tf.name_scope('weights'):
      weights = weight_variable([input_dim, output_dim])
      variable_summaries(weights)
    with tf.name_scope('biases'):
      biases = bias_variable([output_dim])
      variable_summaries(biases)
    with tf.name_scope('Wx_plus_b'):
      preactivate = tf.matmul(input_tensor, weights) + biases
      tf.summary.histogram('pre_activations', preactivate)
    activations = act(preactivate, name='activation')
    tf.summary.histogram('activations', activations)
    return activations

#x_loaded, y_loaded = read_and_decode_single_example("denseData_2017-11-07_10-31-05.tfrecords")#denseData_2017-11-06_08-58-03.tfrecords")

filename_list = ["denseData_2017-11-07_10-31-05.tfrecords", "denseData_2017-11-06_08-58-03.tfrecords"]
x_loaded, y_loaded = read_and_decode_single_example(filename_list)

# groups examples into batches randomly
x_batch, y_batch = tf.train.shuffle_batch(
    [x_loaded, y_loaded], batch_size=10000,
    capacity=2000000,
    min_after_dequeue=50000,
    enqueue_many=False, # I think this might be wrong.
    allow_smaller_final_batch=False,
    name='rand_batch')

# We build our small model: a basic two layers neural net with ReLU
with tf.name_scope('input'):
    x = tf.placeholder(tf.float32, shape=[None, 72], name='x-input')
    y_true = tf.placeholder(tf.int32, shape=[None, 1], name='y-input')


layer1 = nn_layer(x, 72, 144, 'layer1')

with tf.name_scope('dropout'):
    keep_prob = tf.placeholder(tf.float32)
    tf.summary.scalar('dropout_keep_probability', keep_prob)
    dropped = tf.nn.dropout(layer1, keep_prob)

layer2 = nn_layer(dropped, 144, 72, 'layer2')

y = nn_layer(layer2, 72, 1, 'layer3')


with tf.name_scope('Loss'):
    loss_op = tf.losses.mean_squared_error(y_true, y)
with tf.name_scope('Accuracy'):
    accuracy = tf.reduce_mean(tf.cast(tf.equal(tf.cast(tf.round(y),tf.int32),tf.cast(y_true,tf.int32)),tf.float32))

np.set_printoptions(threshold=np.nan) # Makes printing not be truncated
# We add the training operation, ...
adam = tf.train.AdamOptimizer(learn_rate)
train_op = adam.minimize(loss_op, name="optimizer")

# Add ops to save and restore all the variables -- checkpoint style
saver = tf.train.Saver()

# Create a summary to monitor cost tensor
tf.summary.scalar("loss", loss_op)
tf.summary.scalar("accuracy", accuracy)
# Merge all summaries into a single op
merged_summary_op = tf.summary.merge_all()

builder = tf.saved_model.builder.SavedModelBuilder(export_dir)

startTime = time.time()
with tf.Session() as sess:
    # ... init our variables, ...
    sess.run(tf.global_variables_initializer())
    tf.train.start_queue_runners(sess=sess)

    # #if os.path.isfile("./tmp/model.ckpt"):
    saver.restore(sess, "./tmp/model.ckpt")
    # print('Loaded checkpoint file')
    #builder.add_meta_graph_and_variables(sess)

    summary_writer = tf.summary.FileWriter("./logs", graph=tf.get_default_graph())
    # ... train ...
    for i in range(2000):
        x_input, y_input = sess.run([x_batch, y_batch])

        _, loss, acc, summary = sess.run([train_op, loss_op, accuracy, merged_summary_op],feed_dict={x: x_input, y_true: y_input, keep_prob: 0.75})

        summary_writer.add_summary(summary, i)

        print('iter:%d - loss:%f - accuracy:%f' % (i, loss, acc))
        if i % 10 == 9:
            save_path = saver.save(sess, "./tmp/model.ckpt")

    # builder.add_meta_graph_and_variables(sess, ["tag"], signature_def_map={
    #     "model": tf.saved_model.signature_def_utils.predict_signature_def(
    #         inputs={"x": x},
    #         outputs={"y": y})
    # })
    # builder.save()
    # for j in range(100):
    #     x_input, y_input = sess.run([x_batch, y_batch])
    #     # Finally, we check our final accuracy
    #     acc = sess.run(accuracy, feed_dict={
    #         x: x_input,
    #         y_true: y_input,
    #         keep_prob: 1
    #     })
    #     print('accuracy:%f' % (acc))
   # builder.save()

print("Time taken: %f" % (time.time() - startTime))