package goals.value_function_learning;

import java.io.IOException;

public class TestPythonNetwork {
    public static void main(String[] args) {
//        ProcessBuilder pb = new ProcessBuilder("python","python/java_value_function/create_graph.py");
        ProcessBuilder pb = new ProcessBuilder("python","python/java_value_function/create_generic_graph.py",
                "--layers",  "72", "125", "36", "1", "--savepath", "src/main/resources/tflow_models/generic_graph.pb");

        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        try {
            Process p = pb.start();

        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
