package data;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

public class MAIN_EliminateDuplicates {

	static String origin = "test3";
	static String destination = "test3";

	public static void main(String[] args) {
		ArrayList<File> qwopFiles = getQWOPFiles();

	}

	/** Get all .qwop files in the working directory. **/
	public static ArrayList<File> getQWOPFiles(){
		File folder = new File(".");
		File[] listOfFiles = folder.listFiles();
		ArrayList<File> qwopFiles = new ArrayList<File>();

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {


				File f = listOfFiles[i];
				int indexOfLastSeparator = f.getName().lastIndexOf(".");

				// Only get the .qwop files
				if (f.getName().substring(indexOfLastSeparator).equalsIgnoreCase(".qwop")) {
					System.out.println("File " + f.getName());
					qwopFiles.add(f);
				}
			} else if (listOfFiles[i].isDirectory()) {
				//System.out.println("Directory " + listOfFiles[i].getName());
			}
		}
		return qwopFiles;
	}

	/** Eliminate all duplicate runs in the given file and save to destination file. **/
	public static void eliminateDuplicates(String origin, String destination) {
		File file = new File(origin + ".qwop");

		if(file.exists()){
			double bytes = file.length();
			double kilobytes = (bytes / 1024);
			double megabytes = (kilobytes / 1024);
			System.out.println(Math.round(megabytes*100)/100. + " megabyte file input: " + origin);
		}

		SaveableFileIO<SaveableSingleGame> io = new SaveableFileIO<SaveableSingleGame>();
		HashSet<SaveableSingleGame> loaded = io.loadObjectsUnordered(origin);
		io.storeObjectsUnordered(loaded, destination, false);


		file = new File(destination + ".qwop");
		if(file.exists()){
			double bytes = file.length();
			double kilobytes = (bytes / 1024);
			double megabytes = (kilobytes / 1024);
			System.out.println(Math.round(megabytes*100)/100. + " megabyte file output: " + destination);
		}
	}
}
