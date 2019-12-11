import tensorflow as tf
import csv
import time

IMG_WIDTH = int(640/5)
IMG_HEIGHT = int(400/5)
pool_reduction = 2
INPUT_COUNT = 35

# tensorboard --samples_per_plugin images=10000 --logdir=summarytmp
def decode_img(img):
  # convert the compressed string to a 3D uint8 tensor
  img = tf.image.decode_png(tf.io.read_file(img), channels=3)
  # Use `convert_image_dtype` to convert to floats in the [0,1] range.
  img = tf.image.convert_image_dtype(img, tf.float32)
  img = tf.image.crop_to_bounding_box(img, 50, 125, 350, 300)
  # resize the image to the desired size.
  return tf.image.resize(img, [50, 50])

img_input = tf.placeholder(tf.string)
img_decoded = decode_img(img_input)

img_decoded = img_decoded - tf.tile(tf.expand_dims(img_decoded[:, 0, :],1), [1, 50, 1])
x = tf.expand_dims(img_decoded, 0) # Dimension 1 is associated with batches.
img_summary0 = tf.summary.image('filter0', x)


def cnn_layer(input, kernel, channelsin, channelsout, name='filter'):
    wc1 = tf.Variable(tf.random.uniform([kernel, kernel, channelsin, channelsout], minval=0, maxval=1))
    bc1 = tf.Variable(tf.random_normal([channelsout]))
    x = tf.nn.conv2d(input, filter=wc1, strides=[1, 1, 1, 1], padding='SAME')
    x = tf.nn.bias_add(x, bc1)
    x = tf.nn.relu(x)
    # x = tf.nn.max_pool(x, ksize=[1, pool_reduction, pool_reduction, 1], strides=[1, pool_reduction, pool_reduction, 1], padding='SAME', name='pool1')
    tf.summary.image(name, x, family='cnn')
    return x

c1 = cnn_layer(x, 7, 3, 3, 'f1')
c2 = cnn_layer(c1, 3, 3, 1, 'f2')
#c3 = cnn_layer(c2, 3, 1, 1, 'f3')
x = tf.nn.max_pool(c2, ksize=[1, 4, 4, 1], strides=[1, 2, 2, 1], padding='SAME', name='pool1')

c4 = cnn_layer(x, 3, 1, 1, 'f4')
#c5 = cnn_layer(c4, 3, 1, 1, 'f5')
x = tf.nn.max_pool(c4, ksize=[1, 4, 4, 1], strides=[1, 2, 2, 1], padding='SAME', name='pool1')
img_summary2 = tf.summary.image('filter2', x)


x = tf.reshape(x, [1, -1]) # Flatten to 1D

wfc1 = tf.Variable(tf.constant(1/(50 * 50 / pool_reduction / pool_reduction), shape = [169, INPUT_COUNT]))
bfc1 = tf.Variable(tf.random_normal([INPUT_COUNT]))
x = tf.matmul(x, wfc1) + bfc1

input_desired = tf.placeholder(dtype=tf.float32, shape=[1, INPUT_COUNT])
loss_op = tf.losses.mean_squared_error(x, input_desired)
tf.summary.scalar("loss", loss_op)

torsoThetaError = x[0][1] - input_desired[0][1]
tf.summary.scalar("torso_theta_err", torsoThetaError)

#rfootPosErr = (x[0][17] - input_desired[0][17])


optimizer = tf.train.AdamOptimizer(learning_rate=1e-4, name='optimizer')
train_op = optimizer.minimize(loss_op, name='train')

# list_ds = tf.data.Dataset.list_files('../vision_capture/run1/*')
# iterator = list_ds.make_one_shot_iterator()

merged_summary_op = tf.summary.merge_all(name="summary")

init = tf.global_variables_initializer()
count = 0
count1 = 0
saver_def = tf.train.Saver()
savepath = './saves/modeldef.pb'
with open(savepath, 'wb') as f:
    f.write(tf.get_default_graph().as_graph_def().SerializeToString())

with tf.Session() as sess:
    sess.run(init)
    summ_writer = tf.summary.FileWriter('./summarytmp/' + str(time.strftime("%Y%m%d-%H%M%S")), sess.graph)
    for i in range(1000):
        for j in range(31):
            with open('../vision_capture/run' + str(j) + '/poses.dat', 'r') as pose_data:
                reader = csv.reader(pose_data, delimiter='\t')
                for p in reader:
                    [loss, thing, summ] = sess.run([loss_op, train_op, merged_summary_op], feed_dict={img_input : '../vision_capture/run' + str(j) + '/' + p[0], input_desired : [p[2:INPUT_COUNT + 2]]})
                    # print(loss)
                    if count + 1 % 10000:
                        save_path = saver_def.save(sess, "./saves/model.ckpt")

                    if count % 200 == 0:
                        summ_writer.add_summary(summ, count1)
                        summ_writer.flush()
                        count1 += 1
                    count += 1


