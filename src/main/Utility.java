package main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Random;

import org.apache.curator.utils.PathUtils;

public class Utility {

	private static long ticTime;
	private static long tocTime;

	/** Random number generator for new node selection **/
	private final static Random rand = new Random();

	/** Generate a random integer between two values, inclusive. **/
	public static int randInt(int min, int max) {
		if (min > max) throw new IllegalArgumentException("Random int sampler should be given a minimum value which is less than or equal to the given max value.");
		int randomNum = rand.nextInt((max - min) + 1) + min;
		return randomNum;
	}

	/** Matlab tic and toc functionality. **/
	public static void tic(){
		ticTime = System.nanoTime();
	}

	public static long toc(){
		tocTime = System.nanoTime();
		long difference = tocTime - ticTime;
		if (difference < 1000000000){
			System.out.println(Math.floor(difference/10000)/100 + " ms elapsed.");
		}else{
			System.out.println(Math.floor(difference/100000000.)/10. + " s elapsed.");
		}
		return difference;
	}

	
	public static void stringToLogFile(String contents, String outPath) throws IOException {
		BufferedWriter writer = null;
		FileWriter fw = null;
		
		try {
			fw = new FileWriter(outPath);
			writer = new BufferedWriter(fw);
			
			writer.write(contents);	
			
		}finally {
			if (writer != null) writer.close();
			if (fw != null) fw.close();
		}
	}
	
	/** Write some part of a file to a log. Begin logging with !LOG_START and end with
	 * !LOG_END. This can be done multiple times in the same file.  **/
	public static void sectionToLogFile(String inPath, String outPath) throws IOException {
		File inFile = new File(inPath);
		File outFile = new File(outPath);

		boolean collecting = false;
		String divider = "**************************************************************";
		BufferedReader reader = null;
		BufferedWriter writer = null;
		FileReader fr = null;
		FileWriter fw = null;
		
		try {
			fr = new FileReader(inPath);
			reader = new BufferedReader(fr);
			fw = new FileWriter(outPath);
			writer = new BufferedWriter(fw);

			while (reader.ready()) {
				String nextLine = reader.readLine();

				if (nextLine.contains("!LOG_START")){
					collecting = true;
					writer.write(divider + "\n");
				}else if(nextLine.contains("!LOG_STOP")) {
					collecting = false;
					writer.write(divider + "\n");
				}

				if (collecting) {
					writer.write(nextLine + "\n");
				}
			}

		}finally {
			if (writer != null) {
				writer.close();
				fw.close();
			}
			if (reader != null) {
				reader.close();	
				fr.close();
			}
		}
	}

	/** Clear out an existing file. **/
	public static void clearExistingFile(String fileName) {
		File file = new File(fileName);
		try {
			boolean result = Files.deleteIfExists(file.toPath());
			if (result) System.out.println("Cleared file: " + file.getName());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/** Generate a filename. Format is: [prefix]_YYYY-MM-DD_HH-mm-ss.[class name]**/
	public static String generateFileName(String prefix, String className) {
		Date date = new Date() ;
		SimpleDateFormat dateFormat = new SimpleDateFormat("'" + prefix + "_'" + "yyyy-MM-dd_HH-mm-ss" + "'." +  className + "'") ;
		String name = dateFormat.format(date);
		System.out.println("Generated file: " + name);

		return name;
	}

	/** Get a timestamp in string form. **/
	public static String getTimestamp() {
		Date date = new Date() ;
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss") ;
		String timestamp = dateFormat.format(date);
		return timestamp;
	}
	
	/** Load a configuration file. **/
	public static Properties loadConfigFile(File file) {
		FileInputStream fis = null;
		Properties prop = new Properties();
		try {
			fis = new FileInputStream(file);
			prop.load(fis);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			if (fis != null)
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		return prop;
	}
	
    public static String getExcutionPath(){
    	String path = "";
    	try {
			path = Utility.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
    	if (path.endsWith(".jar")) { // Executing from packaged jar.
    		int lastDiv = path.lastIndexOf("/");
    		path = path.substring(0, lastDiv + 1);
    		path = path.replaceFirst("^/(.:/)", "$1"); // Needed to strip drive letter off of the front of the path in Windows.
    		path = path.replace("//", ""); // If we end up with a double slashed concatenated after all these replacements.
    	}else { // Running in eclipse or something.
    		path += "../../";
    	}
        return path;
    }
}




