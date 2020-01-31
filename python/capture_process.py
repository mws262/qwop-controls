import tensorflow as tf
import csv
import time
import numpy as np

INPUT_COUNT = 35
BATCH_SIZE = 25

# Crop of the 640x400 total game size.
# Important! Pick a width divisible by 8 or the screen streamer won't produce correctly-sized frames.
# (height offset to left top corner, width offset to left top corner, height, width)
qwop_crop = (60, 100, 336, 336)
tf.constant(qwop_crop, name="crop_dim")

# Cropped QWOP capture can get rescaled (or not).
img_resize_dims = (qwop_crop[2], qwop_crop[3]) # Height then width!
tf.constant(img_resize_dims, name="img_resize_dims")

##################

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

# min_st = tf.reduce_min(state_labels_tensor, axis=0)
# max_st = tf.reduce_max(state_labels_tensor, axis=0)
# with tf.Session() as sess:
#     mins, maxes = sess.run([min_st, max_st])
# np.savetxt("mins.txt", mins, delimiter='\n')
# np.savetxt("maxes.txt", maxes, delimiter='\n')

min_states = tf.constant(np.loadtxt("mins.txt"), dtype=tf.float32, name="mins")
max_states = tf.constant(np.loadtxt("maxes.txt"), dtype=tf.float32, name="maxes")
range_states = max_states - min_states

####################

initializer = tf.glorot_uniform_initializer(seed=6)

# tensorboard --samples_per_plugin images=10000 --logdir=summarytmp
def decode_img(img, label):
  # convert the compressed string to a 3D uint8 tensor
  img = tf.image.decode_png(tf.io.read_file(img), channels=3)
  # Use `convert_image_dtype` to convert to floats in the [0,1] range.
  img = tf.image.convert_image_dtype(img, tf.float32)
  img = tf.image.crop_to_bounding_box(img, qwop_crop[0], qwop_crop[1], qwop_crop[2], qwop_crop[3])
  # resize the image to the desired size.
  img = tf.image.resize(img, img_resize_dims)
  # img = img - tf.tile(tf.expand_dims(img[:, 0, :],1), [1, imgdim, 1]) # Subtract out the background sort of.

  label_norm = (label - min_states) / range_states
  return img, label_norm

def cnn_layer(input, kernel, channelsin, channelsout, name='filter'):
    with tf.name_scope(name):
        wc1 = tf.Variable(initializer([kernel, kernel, channelsin, channelsout]))
        bc1 = tf.Variable(initializer([channelsout]))
        x = tf.nn.conv2d(input, filter=wc1, strides=[1, 1, 1, 1], padding='SAME')
        x = tf.nn.bias_add(x, bc1)
        x = tf.nn.relu(x)
        # x = tf.nn.max_pool(x, ksize=[1, pool_reduction, pool_reduction, 1], strides=[1, pool_reduction, pool_reduction, 1], padding='SAME', name='pool1')
        for i in range(channelsout):
            tf.summary.image(name, tf.expand_dims(x[:,:,:,i],3), family='cnn', max_outputs=1)
        return x

#######################


dataset = tf.data.Dataset.from_tensor_slices((img_paths_tensor, state_labels_tensor))
dataset = dataset.repeat().shuffle(epoch_size)

dataset = dataset.map(decode_img, num_parallel_calls=8)
dataset = dataset.batch(BATCH_SIZE)
# dataset = dataset.prefetch(1)

images, labels = dataset.make_one_shot_iterator().get_next()
#########################


file_in = tf.placeholder(tf.string, name="img_filename")
st_in = tf.placeholder(tf.float32, name="st_in")
img_decoded, _ = decode_img(file_in, st_in)
img_decoded = tf.expand_dims(img_decoded, 0) # Dimension 1 is associated with batches.
img_out = tf.identity(img_decoded, name="processed_img")
st_in = tf.identity(st_in, name="state_normed")


# Version intended for feeding in captures from java vs training mode
eval_mode = True
if eval_mode:
    input_eval = tf.placeholder(tf.uint8, shape=[img_resize_dims[0], img_resize_dims[1], 3], name="input_eval")
    tf.image.encode_png(input_eval, name="png")
    img = tf.image.convert_image_dtype(input_eval, tf.float32)
    img = tf.expand_dims(img, axis=0)
else:
    img = images

x = tf.placeholder_with_default(img, shape=None, name="img_in")

x = cnn_layer(x, 5, 3, 12, 'f1')
x = cnn_layer(x, 3, 12, 12, 'f2')

x = tf.nn.max_pool(x, ksize=[1, 8, 8, 1], strides=[1, 8, 8, 1], padding='SAME', name='pool1')
x = cnn_layer(x, 3, 12, 36, 'f4')
#x = cnn_layer(x, 3, 1, 1, 'f4')
x = tf.nn.max_pool(x, ksize=[1, 8, 8, 1], strides=[1, 8, 8, 1], padding='SAME', name='pool2')
x = cnn_layer(x, 3, 36, 36, 'f4')
#x = cnn_layer(x, 3, 1, 1, 'f4')
x = tf.nn.avg_pool(x, ksize=[1, 8, 8, 1], strides=[1, 8, 8, 1], padding='SAME', name='pool3')

#x = cnn_layer(x, 3, 1, 1, 'f5')
#c5 = cnn_layer(c4, 3, 1, 1, 'f5')
#x = tf.nn.max_pool(x, ksize=[1, 4, 4, 1], strides=[1, 4, 4, 1], padding='SAME', name='pool1')
# img_summary2 = tf.summary.image('filter2', x[:,:,:,0:3], max_outputs=1)
for i in range(36):
    tf.summary.image("pool", tf.expand_dims(x[:, :, :, i],3), family='cnn', max_outputs=1)

x = tf.reshape(x, [-1, 36])#int(imgdim * imgdim / 64)]) # Flatten to 1D

wfc1 = tf.Variable(initializer([36, INPUT_COUNT]))
bfc1 = tf.Variable(initializer([INPUT_COUNT]))
x = tf.matmul(x, wfc1) + bfc1
# x = tf.nn.relu(x)
#
# wfc2 = tf.Variable(tf.constant(0.01, shape = [48, INPUT_COUNT]))
# bfc2 = tf.Variable(tf.random_normal([INPUT_COUNT]))
# x = tf.matmul(x, wfc2) + bfc2
x = tf.identity(x, name="prediction")

rescaled_states = x * range_states + min_states
rescaled_states = tf.identity(rescaled_states, name="prediction_rescaled")

# input_desired = tf.placeholder(dtype=tf.float32, shape=[1, INPUT_COUNT])
loss_op = tf.losses.mean_squared_error(x, labels)
tf.summary.scalar("loss", loss_op)

# torsoThetaError = x[0][1] - input_desired[0][1]
# tf.summary.scalar("torso_theta_err", torsoThetaError)

optimizer = tf.train.AdamOptimizer(learning_rate=1e-3, name='optimizer')
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

if not eval_mode:
    with tf.device('/GPU:0'):
        with tf.Session(config=tf.ConfigProto(log_device_placement=True)) as sess:
            sess.run(init)
            saver_def.restore(sess, tf.train.latest_checkpoint("./saves/"))
            summ_writer = tf.summary.FileWriter('./summarytmp/' + str(time.strftime("%Y%m%d-%H%M%S")), sess.graph)
            for i in range(int(1e9)):

                # xo, lo, xx = sess.run([x, labels, xp2])
                if count % 50 == 0:
                    [loss, thing, summ] = sess.run([loss_op, train_op,
                                                    merged_summary_op])
                    summ_writer.add_summary(summ, count1)
                    summ_writer.flush()
                    count1 += 1
                    print(count)
                else:
                    sess.run([loss_op, train_op])
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


