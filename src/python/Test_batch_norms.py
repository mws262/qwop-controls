import tensorflow as tf
import numpy as np
import normalization as norm


a = np.reshape(np.array(range(20)),[1,-1,5])

tf_in = tf.placeholder(tf.float32,[None,4,5])
normed = tf.layers.batch_normalization(tf_in, axis=2,center=True, scale=True, training=True)
initial = tf.truncated_normal([5,1], stddev=0.1)
W = tf.Variable(initial)
#normed = norm.batch_norm(tf_in,True,internal_update=True)
out = tf.matmul(tf.squeeze(tf_in),W)





adam = tf.train.AdamOptimizer(0.001)
update_ops = tf.get_collection(tf.GraphKeys.UPDATE_OPS)

with tf.control_dependencies(update_ops):
    train_op = adam.minimize(out, name="optimizer")

print a
with tf.Session() as sess:

    sess.run(tf.global_variables_initializer())

    for i in range(100):

        normed_raw,_ = sess.run([normed,train_op], feed_dict={tf_in: a})
        print normed_raw