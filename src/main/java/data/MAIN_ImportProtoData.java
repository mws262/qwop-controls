package data;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;

import data.DenseDataProtos.DataSet;
import data.DenseDataProtos.DataSet.DenseData;

public class MAIN_ImportProtoData {

    public static void main(String[] args) {
        String filename = "denseData_2017-11-06_08-58-03.proto";
        DataSet dataValidate = null;
        try (FileInputStream fIn = new FileInputStream(filename)) {
            dataValidate = DataSet.parseFrom(fIn);
        } catch (IOException e) {
            e.printStackTrace();
        }
        DenseData.State state1 = Objects.requireNonNull(dataValidate).getDenseData(0).getState(0);
        float th1 = state1.getHead().getX();
        DenseData.State state2 = dataValidate.getDenseData(10).getState(0);
        float th2 = state2.getBody().getTh();
        System.out.println(th1 + ", " + th2);
    }
}
