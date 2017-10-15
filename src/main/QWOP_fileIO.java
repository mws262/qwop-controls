package main;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class QWOP_fileIO<T> {

	public void storeObjects(ArrayList<T> data, String filenameNoExtension, boolean append){

		OutputStream ops = null;
		ObjectOutputStream objOps = null;

		try {
			if (append && new File(filenameNoExtension + ".qwop").isFile()){
				ops = new FileOutputStream(filenameNoExtension + ".qwop",true);
				objOps = new AppendingObjectOutputStream(ops);
			}else{
				ops = new FileOutputStream(filenameNoExtension + ".qwop",false);
				objOps = new ObjectOutputStream(ops);	
			}

			for (T d : data){
				objOps.writeObject(d);
			}
			objOps.flush();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			try{
				if(objOps != null) objOps.close();
			} catch (Exception ex){
			}
		}
	}


	public void storeObjects(T data,String filenameNoExtension, boolean append){
		ArrayList<T> dataList = new ArrayList<T>();
		dataList.add(data);
		storeObjects(dataList,filenameNoExtension,append);
	}

	public ArrayList<T> loadObjects(String filenameNoExtension){

		InputStream fileIs = null;
		ObjectInputStream objIs = null;
		int counter = 0;

		ArrayList<T> dataList = new ArrayList<T>();
		try {
			final String dir = System.getProperty("user.dir");
	        System.out.println("current directory: " + dir);
			fileIs = new FileInputStream(filenameNoExtension + ".qwop");
			objIs = new ObjectInputStream(fileIs);
			boolean reading = true;
			while (reading){
				try{
					T obj = (T) objIs.readObject().getClass();
					dataList.add(obj);
					counter++;
				}catch(EOFException c){
					reading = false;
				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			try {
				if(objIs != null) objIs.close();
			} catch (Exception ex){

			}
		}
		System.out.println("Loaded " + counter + " objects from file " + filenameNoExtension + ".");
		return dataList;
	}

}