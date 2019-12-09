import tensorflow as tf
import csv

IMG_WIDTH = int(640/10)
IMG_HEIGHT = int(400/10)
pool_reduction = 2

def decode_img(img):
  # convert the compressed string to a 3D uint8 tensor
  img = tf.image.decode_png(tf.io.read_file(img), channels=3)
  # Use `convert_image_dtype` to convert to floats in the [0,1] range.
  img = tf.image.convert_image_dtype(img, tf.float32)
  # resize the image to the desired size.
  return tf.image.resize(img, [IMG_WIDTH, IMG_HEIGHT])

img_input = tf.placeholder(tf.string)
img_decoded = decode_img(img_input)

x = tf.expand_dims(img_decoded, 0) # Dimension 1 is associated with batches.
wc1 = tf.Variable(tf.random_normal([5, 5, 3, 1]))
bc1 = tf.Variable(tf.random_normal([1]))
x = tf.nn.conv2d(x, filter=wc1, strides=[1, 1, 1, 1], padding='SAME')
x = tf.nn.bias_add(x, bc1)
x = tf.nn.relu(x)
x = tf.nn.max_pool(x, ksize=[1, pool_reduction, pool_reduction, 1], strides=[1, pool_reduction, pool_reduction, 1], padding='SAME')
x = tf.reshape(x, [1, -1]) # Flatten to 1D

wfc1 = tf.Variable(tf.random_normal([int(IMG_HEIGHT * IMG_WIDTH / pool_reduction / pool_reduction), 1]))
bfc1 = tf.Variable(tf.random_normal([1]))
x = tf.matmul(x, wfc1) + bfc1

input_desired = tf.placeholder(dtype=tf.float32, shape=[1, 1])
loss_op = tf.losses.mean_squared_error(x, input_desired)

optimizer = tf.train.AdamOptimizer(learning_rate=1e-2, name='optimizer')
train_op = optimizer.minimize(loss_op, name='train')

# list_ds = tf.data.Dataset.list_files('../vision_capture/run1/*')
# iterator = list_ds.make_one_shot_iterator()

init = tf.global_variables_initializer()
with tf.Session() as sess:
    sess.run(init)
    for i in range(1000):
        with open('../vision_capture/run1/poses.dat', 'r') as pose_data:
            reader = csv.reader(pose_data, delimiter='\t')
            for p in reader:
                [loss, thing] = sess.run([loss_op, train_op], feed_dict={img_input : '../vision_capture/run1/' + p[0], input_desired : [[p[2]]]})
                print(loss)



