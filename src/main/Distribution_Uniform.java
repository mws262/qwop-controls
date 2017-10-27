package main;

import java.util.ArrayList;

public class Distribution_Uniform extends Distribution<Action> {
	
	public Distribution_Uniform() {}

	@Override
	public Action randOnDistribution(ArrayList<Action> set) {
		return randSample(set);
	}	
}
