import tensorflow as tf
import csv
import time
import numpy as np

INPUT_COUNT = 35
BATCH_SIZE = 25

# Crop of the 640x400 total game size.

# Important: This is a problem for using the network after training. The net will be semi-useless for the capture
# dimension if the following isn't heeded.
# gstreamer sends data to a buffer. This buffer wants to be in blocks of 4096 bytes. It will round up if the
# required bytes are between. This freaks out the buffer-consumer. There may be a way to reconfigure the consumers to
# ignore extra bytes (vlc does it fine, but ffmpeg does not with default settings), but
# a simpler approach is to make the total number of capture bytes divisible by 4096. Total bytes is normally
# 3 x pixel length x pixel height for 24 bit rgb colors. That means that the prime factorization of the dimensions
# must contain 2^12. For a square capture, this means multiples of 2^6 (64).
#
# TL;DR: If picking a square capture region, make the dimensions multiples of 64.
#
# Run this line in terminal from the qwop-controls/python directory to get training progress showing up in the web browser.
# tensorboard --samples_per_plugin images=10000 --logdir=summarytmp

# (height offset to left top corner, width offset to left top corner, height, width)
qwop_crop = (68, 112, 320, 320)
tf.constant(qwop_crop, name="crop_dim") # Exists purely so it can be read by the consumer of this model.

# Cropped QWOP capture can get rescaled (or not). Recommend not for now. I don't know for sure whether this will do the
# rescaling in the same way as other image sources.
img_resize_dims = (qwop_crop[2], qwop_crop[3]) # Height then width!
tf.constant(img_resize_dims, name="img_resize_dims") # Exists purely so it can be read by the consumer of this model.


# Read in the captured training data acquired by running slightly randomized controllers on the game through Java.
img_paths = [] # Paths of the training images e.g. ../vision_capture/run0/ts0.png
state_labels = [] # Pose information -- the x, y, thetas for all the body parts.
for j in range(31): # TODO: don't hardcode the number of training runs available... god.
    with open('../vision_capture/run' + str(j) + '/poses.dat', 'r') as pose_data: # Each run has a plaintext file containing image names and the corresponding pose info on the same line with tab delimiters.
        reader = csv.reader(pose_data, delimiter='\t')
        for p in reader:
            img_paths.append('../vision_capture/run' + str(j) + '/' + p[0])
            state_labels.append([float(i) for i in p[2:INPUT_COUNT + 2]]) # First index is image name. Skip that. Also skip index 1. It is x-position of the torso which cannot be inferred from a single image.

epoch_size = len(img_paths) # Feeds in every training image available.
img_paths_tensor = tf.convert_to_tensor(img_paths, dtype=tf.string)
state_labels_tensor = tf.convert_to_tensor(state_labels, dtype=tf.float32)

# Can recalculate the rescaling values for the training data. It is saved as txt. Shouldn't be necessary unless more data is added.
# min_st = tf.reduce_min(state_labels_tensor, axis=0)
# max_st = tf.reduce_max(state_labels_tensor, axis=0)
# with tf.Session() as sess:
#     mins, maxes = sess.run([min_st, max_st])
# np.savetxt("mins.txt", mins, delimiter='\n')
# np.savetxt("maxes.txt", maxes, delimiter='\n')

# Overall stats for the training data. Used for rescaling all states from 0 to 1.
min_states = tf.constant(np.loadtxt("mins.txt"), dtype=tf.float32, name="mins")
max_states = tf.constant(np.loadtxt("maxes.txt"), dtype=tf.float32, name="maxes")
range_states = max_states - min_states

initializer = tf.glorot_uniform_initializer(seed=6) # TODO: is this the best option?

# Load one of the saved training images and its corresponding pose data. Part of the input pipeline.
def decode_img(img_name, pose_values):
  # convert the compressed string to a 3D uint8 tensor
  img_name = tf.image.decode_png(tf.io.read_file(img_name), channels=3)
  img_name = tf.image.convert_image_dtype(img_name, tf.float32) # Takes images with 0-255 UINT8 values to floats from 0-1
  img_name = tf.image.crop_to_bounding_box(img_name, qwop_crop[0], qwop_crop[1], qwop_crop[2], qwop_crop[3]) # Pull out a cropped chunk from the image. Just the area of interest around the runner.
  img_name = tf.image.resize(img_name, img_resize_dims) # Do a further rescaling if img_resize_dims are different from the cropped image's.
  label_norm = (pose_values - min_states) / range_states # Rescale the pose data to be from 0 to 1.
  return img_name, label_norm

# Convenience function for creating a convolutional neural network layer (a trainable image filter).
# input - Image tensor (or intermediate) to be filtered.
# filter_dim - The size in pixels of the filter to be "slid" across the image. This window is square with this dimension. I don't know if there's a good reason to do non-square, but I'm not allowing here so far.
# channels_in - Each CNN layer can do multiple filterings of the the same input, presumably picking different features from that single input. This must match the input provided.
# channels_out - The number of different filterings of the inputs.
def cnn_layer(input, filter_dim, channels_in, channels_out, name='filter'):
    with tf.name_scope(name):
        wc1 = tf.Variable(initializer([filter_dim, filter_dim, channels_in, channels_out]))
        bc1 = tf.Variable(initializer([channels_out]))
        x = tf.nn.conv2d(input, filter=wc1, strides=[1, 1, 1, 1], padding='SAME') # This is the number of pixels by which the window of the filter is "slid." IDK if there's a good reason to change this to something besides 1.
        x = tf.nn.bias_add(x, bc1)
        x = tf.nn.relu(x) # Apply the nonlinearity.

        # Pooling could be done here to reduce size of the image. Pooling would just take blocks of pixels and reduce them to a single representative one (whether by average, max, min, etc).
        # x = tf.nn.max_pool(x, ksize=[1, pool_reduction, pool_reduction, 1], strides=[1, pool_reduction, pool_reduction, 1], padding='SAME', name='pool1')

        # Add summary images so the intermediate steps between the filters can be seen in tensorboard. TODO is this slowing things down much?
        for i in range(channels_out):
            tf.summary.image(name, tf.expand_dims(x[:,:,:,i],3), family='cnn', max_outputs=1)
        return x


# Set up the input pipeline for training. TODO optimize this. I haven't spent much time seeing what the bottlenecks are in the training process.
dataset = tf.data.Dataset.from_tensor_slices((img_paths_tensor, state_labels_tensor))
dataset = dataset.repeat().shuffle(epoch_size)
dataset = dataset.map(decode_img, num_parallel_calls=8)
dataset = dataset.batch(BATCH_SIZE)
# dataset = dataset.prefetch(1)
images, labels = dataset.make_one_shot_iterator().get_next()

# Can be used to feed image file names in and see how they load. Not really necessary for the final product.
# file_in = tf.placeholder(tf.string, name="img_filename")
# st_in = tf.placeholder(tf.float32, name="st_in")
# img_decoded, _ = decode_img(file_in, st_in)
#
# img_decoded = tf.expand_dims(img_decoded, 0) # Dimension 1 is associated with batches.
# img_out = tf.identity(img_decoded, name="processed_img")
# st_in = tf.identity(st_in, name="state_normed")

# Version intended for feeding in captures from java vs training mode TODO find a way to not have to rerun this with the flag different in order to evaluate the net.
eval_mode = True
if eval_mode:
    input_eval = tf.placeholder(tf.uint8, shape=[img_resize_dims[0], img_resize_dims[1], 3], name="input_eval")
    # tf.image.encode_png(input_eval, name="png") # Just for seeing what the passed in image looks like (e.g. find wrong colors, etc).
    img = tf.image.convert_image_dtype(input_eval, tf.float32)
    img = tf.expand_dims(img, axis=0)
else:
    img = images

x = tf.placeholder_with_default(img, shape=None, name="img_in")

x = cnn_layer(x, 5, 3, 12, 'f1')
x = cnn_layer(x, 3, 12, 12, 'f2')
x = tf.nn.avg_pool(x, ksize=[1, 8, 8, 1], strides=[1, 8, 8, 1], padding='SAME', name='pool1')
x = cnn_layer(x, 3, 12, 18, 'f4')
x = tf.nn.avg_pool(x, ksize=[1, 8, 8, 1], strides=[1, 8, 8, 1], padding='SAME', name='pool2')
x = cnn_layer(x, 3, 18, 18, 'f4')
x = tf.nn.avg_pool(x, ksize=[1, 8, 8, 1], strides=[1, 8, 8, 1], padding='SAME', name='pool3')

#x = cnn_layer(x, 3, 1, 1, 'f5')
#c5 = cnn_layer(c4, 3, 1, 1, 'f5')
#x = tf.nn.max_pool(x, ksize=[1, 4, 4, 1], strides=[1, 4, 4, 1], padding='SAME', name='pool1')
# img_summary2 = tf.summary.image('filter2', x[:,:,:,0:3], max_outputs=1)
# for i in range(18):
#     tf.summary.image("pool", tf.expand_dims(x[:, :, :, i],3), family='cnn', max_outputs=1)

x = tf.reshape(x, [-1, 18]) #int(imgdim * imgdim / 64)]) # Flatten to 1D

wfc1 = tf.Variable(initializer([18, INPUT_COUNT]))
bfc1 = tf.Variable(initializer([INPUT_COUNT]))
x = tf.matmul(x, wfc1) + bfc1
x = tf.identity(x, name="prediction")

rescaled_states = x * range_states + min_states
rescaled_states = tf.identity(rescaled_states, name="prediction_rescaled") # Scale back to original state scalings for the sake of when we use for evaluation.

loss_op = tf.losses.mean_squared_error(x, labels)
tf.summary.scalar("loss", loss_op)


optimizer = tf.train.AdamOptimizer(learning_rate=1e-3, name='optimizer')
train_op = optimizer.minimize(loss_op, name='train')


merged_summary_op = tf.summary.merge_all(name="summary")

init = tf.global_variables_initializer()
count = 0
count1 = 0
saver_def = tf.train.Saver()

# Save model file for use externally.
savepath = './saves/modeldef.pb'
with open(savepath, 'wb') as f:
    f.write(tf.get_default_graph().as_graph_def().SerializeToString())

if not eval_mode:
    with tf.device('/GPU:0'):
        with tf.Session(config=tf.ConfigProto(log_device_placement=True)) as sess:
            sess.run(init)
            # saver_def.restore(sess, tf.train.latest_checkpoint("./saves/"))
            summ_writer = tf.summary.FileWriter('./summarytmp/' + str(time.strftime("%Y%m%d-%H%M%S")), sess.graph)
            for i in range(int(1e9)):
                # xo, lo, xx = sess.run([x, labels, xp2])
                if count % 50 == 0:
                    [loss, thing, summ] = sess.run([loss_op, train_op, merged_summary_op])
                    summ_writer.add_summary(summ, count1)
                    summ_writer.flush()
                    count1 += 1
                    print(count)
                else:
                    sess.run([loss_op, train_op])
                if count + 1 % 100:
                    save_path = saver_def.save(sess, "./saves/model.ckpt")
                count += 1
