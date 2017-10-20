package data;
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
import java.util.HashSet;

public class SaveableFileIO<T> {

	/** Store objects in ordered form. Only use if you require ordered. It's slower. Also preserves duplicates for better or worse. **/
	public void storeObjectsOrdered(ArrayList<T> data, String filenameNoExtension, boolean append){

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

	/** Store objects in ordered form. Only use if you require ordered. It's slower. Also preserves duplicates for better or worse. **/
	public void storeObjectsOrdered(T data,String filenameNoExtension, boolean append){
		ArrayList<T> dataList = new ArrayList<T>();
		dataList.add(data);
		storeObjectsOrdered(dataList,filenameNoExtension,append);
	}

	/** Store objects in unordered form. Will also not preserve duplicates. **/
	public void storeObjectsUnordered(HashSet<T> data, String filenameNoExtension, boolean append){

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
	
	/** Store objects in unordered form. Will also not preserve duplicates. **/
	public void storeObjectsUnordered(T data,String filenameNoExtension, boolean append){
		HashSet<T> dataList = new HashSet<T>();
		dataList.add(data);
		storeObjectsUnordered(dataList,filenameNoExtension,append);
	}
	
	/** Load objects in unordered form. Will also NOT contain duplicates. **/
	public HashSet<T> loadObjectsUnordered(String filenameNoExtension){
		InputStream fileIs = null;
		ObjectInputStream objIs = null;
		int counter = 0;

		HashSet<T> dataList = new HashSet<T>();
		try {
			final String dir = System.getProperty("user.dir");
	        System.out.println("current directory: " + dir);
			fileIs = new FileInputStream(filenameNoExtension + ".qwop");
			objIs = new ObjectInputStream(fileIs);
			boolean reading = true;
			while (reading){
				try{
					@SuppressWarnings("unchecked")
					T obj = (T) objIs.readObject();
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
	
	/** Load objects in ordered form. Faster but could contain duplicates. **/
	public ArrayList<T> loadObjectsOrdered(String filenameNoExtension){

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
					@SuppressWarnings("unchecked")
					T obj = (T) objIs.readObject();
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

	/**
	 * Handle when we want to append to a save file rather than start anew.
	 * @author matt
	 *
	 */
	public class AppendingObjectOutputStream extends ObjectOutputStream {
		  public AppendingObjectOutputStream(OutputStream out) throws IOException {
		    super(out);
		  }

		  @Override
		  protected void writeStreamHeader() throws IOException {
		    // do not write a header, but reset:
		    // this line added after another question
		    // showed a problem with the original
		    reset();
		  }

		}
}