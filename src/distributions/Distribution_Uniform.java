package distributions;

import java.util.ArrayList;

import main.Action;
import main.Distribution;

public class Distribution_Uniform extends Distribution<Action> {
	
	public Distribution_Uniform() {}

	@Override
	public Action randOnDistribution(ArrayList<Action> set) {
		return randSample(set);
	}	
}
