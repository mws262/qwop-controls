import create_generic_graph as tflow_utility
import tensorflow as tf

# TODO hardcoded sizes and parameters need to be arguments
learning_rate = 1e-3
input_size = 4
output_size = 2
## Define the network
# StateQWOP input
input = tf.placeholder(tf.float32, shape=(None, input_size), name='input')
# Action target
output_target = tf.placeholder(tf.float32, shape=(None, output_size), name='output_target')  # Some values, same dim as the output layer, used in figuring out the loss.
# Q target
scalar_target = tf.placeholder(tf.float32, shape=(None, ), name="scalar_target")  # Some scalar value tied up in figuring out the loss.

# Fully-connected layers pre-processing the input states.
pre_split_output = tflow_utility.sequential_layers(input, (input_size, 6, 4), "fully_connected", tf.nn.elu)

# Net splits into two branches, one set of layers estimating the value, and the other estimating advantage.
value = tflow_utility.sequential_layers(pre_split_output, (4, 2, 1), "value", tf.nn.elu)
advantage = tflow_utility.sequential_layers(pre_split_output, (4, 4, output_size), "advantage", tf.nn.elu)

# Combine value and advantage into Q.
output = value + tf.subtract(advantage, tf.reduce_mean(advantage, axis=1, keepdims=True))
ouput = tf.identity(output, name="output")

# Mask Q by the applicable command.
Qscalar = tf.reduce_sum(output * output_target, axis=1)

# TODO PER weighting

loss = tf.reduce_mean(tf.squared_difference(scalar_target, Qscalar), name="loss")
optimizer = tf.train.AdamOptimizer(learning_rate).minimize(loss, name="train")

init = tf.global_variables_initializer()
saver_def = tf.train.Saver().as_saver_def()
savepath = "../src/main/resources/tflow_models/dqn.pb"
with open(savepath, 'wb') as f:
    f.write(tf.get_default_graph().as_graph_def().SerializeToString())
