import tensorflow as tf

state_dimension = 72

# Input state
state_in = tf.placeholder(tf.float32, shape=(None, state_dimension), name='state_in')

# Desired predicted value. Only used for training.
value_target = tf.placeholder(tf.float32, shape=(None, 1), name='value_target')

hidden_layer_dims = (100, 100)
with tf.name_scope('input_layer'):
    W_init = tf.truncated_normal(shape=[state_dimension, hidden_layer_dims[0]], stddev=0.1, name='weight_init')
    b_init = tf.constant(0.1, shape=[hidden_layer_dims[0]], name='bias_init')
    W = tf.Variable(W_init, name='weights')
    b = tf.Variable(b_init, name='biases')
    pre_activation = tf.matmul(state_in, W) + b
    activations = tf.nn.relu(pre_activation, name='activations')


with tf.name_scope('hidden_layer'):
    W_init = tf.truncated_normal(shape=[hidden_layer_dims[0], hidden_layer_dims[1]], stddev=0.1, name='weight_init')
    b_init = tf.constant(0.1, shape=[hidden_layer_dims[1]], name='bias_init')
    W = tf.Variable(W_init, name='weights')
    b = tf.Variable(b_init, name='biases')
    pre_activation = tf.matmul(activations, W) + b
    activations = tf.nn.relu(pre_activation, name='activations')


with tf.name_scope('output_layer'):
    W_init = tf.truncated_normal(shape=[hidden_layer_dims[1], 1], stddev=0.1, name='weight_init')
    b_init = tf.constant(0.1, shape=[1], name='bias_init')
    W = tf.Variable(W_init, name='weights')
    b = tf.Variable(b_init, name='biases')
    output = tf.matmul(activations, W) + b
    #activations = tf.nn.relu(pre_activation, name='activations')

value = tf.identity(output, name='value_out')

with tf.name_scope('training'):
    loss = tf.reduce_mean(tf.square(value - value_target), name='loss')
    optimizer = tf.train.AdamOptimizer(learning_rate=0.001, name='optimizer')
    train_op = optimizer.minimize(loss, name='train')

init = tf.global_variables_initializer()
saver_def = tf.train.Saver().as_saver_def()

with open('graph.pb', 'w') as f:
    f.write(tf.get_default_graph().as_graph_def().SerializeToString())