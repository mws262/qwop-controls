import tensorflow as tf
import csv
import time
print(tf.__version__)

IMG_WIDTH = int(640/5)
IMG_HEIGHT = int(400/5)
pool_reduction = 2
INPUT_COUNT = 35

imgdim = 64
batch_size = 50
# tensorboard --samples_per_plugin images=10000 --logdir=summarytmp
def decode_img(img, label):
  # convert the compressed string to a 3D uint8 tensor
  img = tf.image.decode_png(tf.io.read_file(img), channels=3)
  # Use `convert_image_dtype` to convert to floats in the [0,1] range.
  img = tf.image.convert_image_dtype(img, tf.float32)
  img = tf.image.crop_to_bounding_box(img, 50, 150, 350, 325)
  # resize the image to the desired size.
  img = tf.image.resize(img, [imgdim, imgdim])
  img = img - tf.tile(tf.expand_dims(img[:, 0, :],1), [1, imgdim, 1]) # Subtract out the background sort of.
  return img, label

def cnn_layer(input, kernel, channelsin, channelsout, name='filter'):
    with tf.name_scope(name):
        wc1 = tf.Variable(tf.random.uniform([kernel, kernel, channelsin, channelsout], minval=0, maxval=2/channelsout))
        bc1 = tf.Variable(tf.random_normal([channelsout]))
        x = tf.nn.conv2d(input, filter=wc1, strides=[1, 1, 1, 1], padding='SAME')
        x = tf.nn.bias_add(x, bc1)
        x = tf.nn.relu(x)
        # x = tf.nn.max_pool(x, ksize=[1, pool_reduction, pool_reduction, 1], strides=[1, pool_reduction, pool_reduction, 1], padding='SAME', name='pool1')
        tf.summary.image(name, x, family='cnn', max_outputs=3)
        return x

#######################
img_paths = []
state_labels = []
for j in range(31):
    with open('../vision_capture/run' + str(j) + '/poses.dat', 'r') as pose_data:
        reader = csv.reader(pose_data, delimiter='\t')
        for p in reader:
            img_paths.append('../vision_capture/run' + str(j) + '/' + p[0])
            state_labels.append([float(i) for i in p[2:INPUT_COUNT + 2]])

epoch_size = len(img_paths)
img_paths_tensor = tf.convert_to_tensor(img_paths, dtype=tf.string)
state_labels_tensor = tf.convert_to_tensor(state_labels, dtype=tf.float32)

dataset = tf.data.Dataset.from_tensor_slices((img_paths_tensor, state_labels_tensor))
dataset = dataset.repeat().shuffle(epoch_size)

dataset = dataset.map(decode_img, num_parallel_calls=8)
dataset = dataset.batch(batch_size)
# dataset = dataset.prefetch(1)

images, labels = dataset.make_one_shot_iterator().get_next()
#########################


file_in = tf.placeholder(tf.string, name="img_filename")
img_decoded, _ = decode_img(file_in, [])
img_decoded = tf.expand_dims(img_decoded, 0) # Dimension 1 is associated with batches.
img_out = tf.identity(img_decoded, name="processed_img")

# img_summary0 = tf.summary.image('filter0', x)

x = tf.placeholder_with_default(images, shape=None, name="img_in")

x = cnn_layer(x, 3, 3, 3, 'f1')
x = cnn_layer(x, 3, 3, 1, 'f2')
x = tf.nn.max_pool(x, ksize=[1, 2, 2, 1], strides=[1, 2, 2, 1], padding='SAME', name='pool1')
x = cnn_layer(x, 3, 1, 1, 'f3')
#x = cnn_layer(x, 3, 1, 1, 'f4')
x = tf.nn.max_pool(x, ksize=[1, 2, 2, 1], strides=[1, 2, 2, 1], padding='SAME', name='pool1')

#x = cnn_layer(x, 3, 1, 1, 'f5')
#c5 = cnn_layer(c4, 3, 1, 1, 'f5')
#x = tf.nn.max_pool(x, ksize=[1, 4, 4, 1], strides=[1, 4, 4, 1], padding='SAME', name='pool1')
img_summary2 = tf.summary.image('filter2', x, max_outputs=3)


x = tf.reshape(x, [-1, int(imgdim * imgdim / 16)]) # Flatten to 1D

wfc1 = tf.Variable(tf.constant(0.01, shape = [int(imgdim * imgdim / 16), 48]))
bfc1 = tf.Variable(tf.constant(0.01, shape=[48]))
x = tf.matmul(x, wfc1) + bfc1
x = tf.nn.relu(x)

wfc2 = tf.Variable(tf.constant(0.01, shape = [48, INPUT_COUNT]))
bfc2 = tf.Variable(tf.random_normal([INPUT_COUNT]))
x = tf.matmul(x, wfc2) + bfc2
x = tf.identity(x, name="prediction")

# input_desired = tf.placeholder(dtype=tf.float32, shape=[1, INPUT_COUNT])
loss_op = tf.losses.mean_squared_error(x, labels)
tf.summary.scalar("loss", loss_op)

# torsoThetaError = x[0][1] - input_desired[0][1]
# tf.summary.scalar("torso_theta_err", torsoThetaError)

optimizer = tf.train.AdamOptimizer(learning_rate=1e-2, name='optimizer')
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

with tf.device('/GPU:0'):
    with tf.Session(config=tf.ConfigProto(log_device_placement=True)) as sess:
        sess.run(init)
        summ_writer = tf.summary.FileWriter('./summarytmp/' + str(time.strftime("%Y%m%d-%H%M%S")), sess.graph)
        for i in range(int(1e9)):
            [loss, thing, summ] = sess.run([loss_op, train_op,
                                            merged_summary_op])
            if count % 50 == 0:
                summ_writer.add_summary(summ, count1)
                summ_writer.flush()
                count1 += 1
                print(count)
            if count + 1 % 100:
                save_path = saver_def.save(sess, "./saves/model.ckpt")
            count += 1

            # for j in range(31):
            #     with open('../vision_capture/run' + str(j) + '/poses.dat', 'r') as pose_data:
            #         reader = csv.reader(pose_data, delimiter='\t')
            #         for p in reader:
            #             [loss, thing, summ] = sess.run([loss_op, train_op, merged_summary_op])#, feed_dict={img_input : '../vision_capture/run' + str(j) + '/' + p[0], input_desired : [p[2:INPUT_COUNT + 2]]})
            #             # print(loss)
            #             if count + 1 % 10000:
            #                 save_path = saver_def.save(sess, "./saves/model.ckpt")

                        # if count % 200 == 0:
                        #     summ_writer.add_summary(summ, count1)
                        #     summ_writer.flush()
                        #     count1 += 1
                        # count += 1


