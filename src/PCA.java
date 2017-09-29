import org.jblas.FloatMatrix;
import org.jblas.Singular;


public class PCA {

	public PCA() {
		// TODO Auto-generated constructor stub
	}
	
	public static void doSVD(FloatMatrix x){
		conditionData(x);
		System.out.println(x);
		FloatMatrix[] USV = Singular.fullSVD(x); // A = U * diag(S) * V'
		System.out.println(USV[0].toString());
		System.out.println(USV[1].toString());
		System.out.println(USV[2].toString()); // Eigenvectors
		System.out.println(USV[1].mul(USV[1]).div(x.rows).toString()); // Eigenvalues
		System.out.println(x.rows + "," + x.columns + "," + USV[2].rows + "," + USV[2].columns);
		
		System.out.println(x.mmul(USV[2])); // Transformation, columns are the different components
		}
	
	/** Subtracts the mean for each variable, and converts to unit variance.
	 *  Samples are rows, variables are columns. Alters matrix x.
	 * @param x
	 */
	public static void conditionData(FloatMatrix x){
		
		for (int i = 0; i < x.columns; i++){
			// Calculate the mean of a column.
			float sum = 0;
			for (int j = 0; j < x.rows; j++){
				sum += x.get(j,i);
			}
			// Subtract the mean out.
			float avg = sum/(float)x.rows;
			for (int j = 0; j < x.rows; j++){
				float centered = x.get(j,i) - avg;
				x.put(j,i,centered);
			}
			
			// Find the standard deviation for each column.
			sum = 0;
			for (int j = 0; j < x.rows; j++){
				sum += x.get(j,i) * x.get(j,i);
			}
			float std = (float)Math.sqrt(sum/(float)(x.rows - 1));
			
			// Divide the standard deviation out.
			for (int j = 0; j < x.rows; j++){
				float unitVar = x.get(j,i)/std;
				x.put(j,i,unitVar);
			}	
		}	
	}

}
