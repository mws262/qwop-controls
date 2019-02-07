package goals.value_function_learning;

import org.tensorflow.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Random;

public class TestTensorflowJavaTraining {


    public static void main(String[] args) {
        byte[] graphDef = null;
        try {
            graphDef = Files.readAllBytes(Paths.get("src/main/resources/tflow_models/generic_graph.pb"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        final String checkpointDir = "src/main/resources/tflow_models/checkpoints";
        final boolean checkpointExists = Files.exists(Paths.get(checkpointDir));

        try (Graph graph = new Graph();
             Session sess = new Session(graph);
             Tensor<String> checkpointPrefix =
                     Tensors.create(Paths.get(checkpointDir, "ckpt").toString())) {
            graph.importGraphDef(graphDef);

            // Initialize or restore.
            // The names of the tensors in the graph are printed out by the program
            // that created the graph:
            // https://github.com/tensorflow/models/blob/master/samples/languages/java/training/model/create_graph.py
            if (checkpointExists) {
                try {
                    sess.runner().feed("save/Const", checkpointPrefix).addTarget("save/restore_all").run();
                } catch(TensorFlowException e) {
                    System.out.println("Checkpoint unable to be loaded. Starting afresh.");
                    sess.runner().addTarget("init").run();
                }
            } else {
                sess.runner().addTarget("init").run();
            }

            // Print all ops in the graph for debugging.
            Iterator<Operation> iter = graph.operations();
            while (iter.hasNext()) {
                System.out.println(iter.next().name());
            }

            // Train a bunch of times.
            // (Will be much more efficient if we sent batches instead of individual values).
            final Random r = new Random();
            r.setSeed(100);

            float[][] infloats = new float[1][72];
            for (int i = 0; i < infloats[0].length; i++) {
                infloats[0][i] = r.nextFloat();
            }
            Tensor<Float> input = Tensors.create(infloats);
            Tensor<Float> value_out = Tensors.create(r.nextFloat());

            for (int i = 0; i < 100; i++) {
                Tensor<Float> output =
                        sess.runner().feed("input", input).feed("output_target", value_out).addTarget("train").fetch(
                                "output").run().get(0).expect(Float.class);

                float[][] outArray = new float[1][1];
                output.copyTo(outArray);
                System.out.println(value_out.floatValue() - outArray[0][0]);
            }
            // Checkpoint.
            // The feed and target name are from the program that created the graph.
            sess.runner().feed("save/Const", checkpointPrefix).addTarget("save/control_dependency").run();
//
//            // Example of "inference" in the same graph:
//            try (Tensor<Float> input = Tensors.create(1.0f);
//                 Tensor<Float> output =
//                         sess.runner().feed("input", input).fetch("output").run().get(0).expect(Float.class)) {
//                System.out.printf(
//                        "For input %f, produced %f (ideally would produce 3*%f + 2)\n",
//                        input.floatValue(), output.floatValue(), input.floatValue());
//            }
        }
    }
}
