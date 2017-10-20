package data;
import java.io.File;
import java.util.HashSet;

public class MAIN_EliminateDuplicates {

	static String filename = "test3";
	
	public static void main(String[] args) {
		
		File file = new File(filename + ".qwop");

		if(file.exists()){
			double bytes = file.length();
			double kilobytes = (bytes / 1024);
			double megabytes = (kilobytes / 1024);
			double gigabytes = (megabytes / 1024);
			System.out.println(Math.round(megabytes*100)/100. + "megabyte file");
		}
		
		SaveableFileIO<SaveableSingleGame> io = new SaveableFileIO<SaveableSingleGame>();
		HashSet<SaveableSingleGame> loaded = io.loadObjectsUnordered("test3");
		
		
		
	}
}
