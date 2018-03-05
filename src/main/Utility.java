package main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

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


}


	
	
