package main;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import data.SparseDataToDense;
import savers.DataSaver_DenseTFRecord;

public class MAIN_ConvertSparseToTFRecord {

	public static void main(String[] args) {
		
		File loadFile = new File("./4_9_18");
		File outFile = new File("./4_9_18_dense");
		outFile.mkdir();
		
		List<File> filesToConvert = new ArrayList<File>();
		File[] files = loadFile.listFiles();
		for (File f : files) {
			if (f.toString().contains("SaveableSingleGame")) {
				filesToConvert.add(f);
			}
		}
		
		DataSaver_DenseTFRecord saver = new DataSaver_DenseTFRecord();
		saver.setSavePath(outFile.getAbsolutePath());
		saver.setSaveInterval(2);
		SparseDataToDense converter = new SparseDataToDense(saver);
		converter.convert(filesToConvert);	
	}
}
