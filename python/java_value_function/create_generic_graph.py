import argparse

import tensorboard
import tensorflow as tf
from tensorflow.core.framework import summary_pb2
from tensorflow.core.util import event_pb2
from tensorflow.python.framework import ops
from tensorflow.python.ops import summary_ops_v2
from tensorflow.python.ops import array_ops
from tensorflow.python.framework import constant_op
from tensorflow.python.framework import dtypes

## Parse command line options defining the network.
parser = argparse.ArgumentParser()
parser.add_argument('-l', '--layers', nargs='+', type=int,
                    help='<Required> Define fully-connected layer sizes from input to output', required=True)
parser.add_argument('-s', '--savepath', type=str, help='<Required> File path and name to save the network graph to.',
                    required=True)
parser.add_argument('-lr', '--learnrate', type=float, help='<Optional> Optimizer learning rate (default = 1e-3).',
                    required=False, default=1e-3)
parser.add_argument('-w', '--weightstd', type=float,
                    help='<Optional> Standard deviation initialization for weights. (default = 0.1).', required=False,
                    default=0.1)
parser.add_argument('-b', '--biasinit', type=float, help='<Optional> Initialization for biases. (default = 0.1).',
                    required=False, default=0.1)
parser.add_argument('-a', '--activations', type=str, help='<Optional> Nonlinear activation type (default leaky_relu).',
                    required=False, default="leaky_relu", choices=['relu', 'leaky_relu', 'sigmoid', 'tanh', 'identity'])
parser.add_argument('-ao', '--activationsout', type=str, help='<Optional> Output layer activation.',
                    required=False, default="identity", choices=['relu', 'leaky_relu', 'sigmoid', 'tanh', 'identity', 'softmax'])

args = parser.parse_args()

# Available activation functions to lookup.
activation_options = {
    "relu": tf.nn.relu,
    "leaky_relu": tf.nn.leaky_relu,
    "sigmoid": tf.nn.sigmoid,
    "tanh": tf.nn.tanh,
    "identity": tf.identity,
    "softmax": tf.nn.softmax
}

# Get user specified options (or defaults).
layer_sizes = args.layers
init_weight_stddev = args.weightstd
init_bias_val = args.biasinit
learning_rate = args.learnrate
activations = activation_options.get(args.activations)
output_activations = activation_options.get(args.activationsout)
savepath = args.savepath


def weight_variable(shape):
    """
    Initialize weight variables for a net layer.

    :param shape: Shape of tensor to create.
    :return: Tensor of weight variables initialized randomly.
    """

    initial = tf.truncated_normal(shape, stddev=init_weight_stddev, name='weight')
    return tf.Variable(initial)


def bias_variable(shape):
    """
    Net layer bias values.

    :param shape: Shape of tensor to create.
    :return: Bias tensor initialized to a constant value.
    """

    initial = tf.constant(init_bias_val, shape=shape, name='bias')
    return tf.Variable(initial)


def variable_summaries(var):
    """
    Attach a lot of summaries to a Tensor (for TensorBoard visualization).

    :param var:
    :return: None
    """

    with tf.name_scope('summaries'):
        mean = tf.reduce_mean(var)
        tf.summary.scalar('mean', mean)
        with tf.name_scope('stddev'):
            stddev = tf.sqrt(tf.reduce_mean(tf.square(var - mean)))
            tf.summary.scalar('stddev', stddev)
            tf.summary.scalar('max', tf.reduce_max(var))
            tf.summary.scalar('min', tf.reduce_min(var))
            tf.summary.histogram('histogram', var)

def nn_layer(input_tensor, input_dim, output_dim, layer_name, act=activations):
    """Reusable code for making a simple neural net layer.

    It does a matrix multiply, bias add, and then uses relu to nonlinearize.
    It also sets up name scoping so that the resultant graph is easy to read,
    and adds a number of summary ops.

    :param input_tensor:
    :param input_dim:
    :param output_dim:
    :param layer_name:
    :param act:
    :return:
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


def sequential_layers(input, layer_sizes, name_prefix):
    """ Create a series of fully-connected layers.

    :param input:
    :param layer_sizes:
    :param name_prefix:
    :return:
    """
    current_tensor = input
    for idx in range(len(layer_sizes) - 1):
        if idx == range(len(layer_sizes) - 1):
            current_tensor = nn_layer(current_tensor, layer_sizes[idx], layer_sizes[idx + 1],
                                      name_prefix + str(idx),
                                      act=tf.identity)  # Note: removed last activation. This needs to be done outside this function. It was problematic with softmax activation since the loss function wants unscaled logits rather than softmaxed values.
        else:
            current_tensor = nn_layer(current_tensor, layer_sizes[idx], layer_sizes[idx + 1],
                                      name_prefix + str(idx))

    return current_tensor


## Define the network
# Input to network.
input = tf.placeholder(tf.float32, shape=(None, layer_sizes[0]), name='input')

# Output target for training.
output_target = tf.placeholder(tf.float32, shape=(None, layer_sizes[-1]), name='output_target')

output = sequential_layers(input, layer_sizes, "fully_connected")

if args.activationsout == "softmax":
    loss = tf.nn.softmax_cross_entropy_with_logits_v2(labels=output_target, logits=output, name='loss')
    output = tf.nn.softmax(output, name='softmax_activation')
else:
    output = output_activations(output, name='output_activation')
    loss = tf.identity(tf.losses.huber_loss(output_target, output), name='loss')
    # loss = tf.reduce_mean(tf.square(output - output_target), name='loss')

output = tf.identity(output, name='output')  # So output gets named correctly in graph definition.
optimizer = tf.train.AdamOptimizer(learning_rate=learning_rate, name='optimizer')
train_op = optimizer.minimize(loss, name='train')

loss_scalar = tf.summary.scalar('loss_function', loss)
tf.summary.histogram('output_distribution', output)
merged_summary_op = tf.summary.merge_all(name="summary")

init = tf.global_variables_initializer()
saver_def = tf.train.Saver().as_saver_def()
with open(savepath, 'wb') as f:
    f.write(tf.get_default_graph().as_graph_def().SerializeToString())

sess = tf.Session(config=tf.ConfigProto(log_device_placement=True))
sess.close()

