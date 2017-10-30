package main;

import java.util.ArrayList;

public class Distribution_Normal extends Distribution<Action> {

	public float mean;
	public float stdev;
	
	public Distribution_Normal(float mean, float stdev) {
		this.mean = mean;
		this.stdev = stdev;	
	}
	
	@Override
	public Action randOnDistribution(ArrayList<Action> set) {
		double r = rand.nextGaussian(); // Gets a new value on bell curve. 0 mean, 1 stddev.
		double rScaled = r*stdev + mean; // Scale to our possible action range.
		Action best = set.get(0);
		float diff = Float.MAX_VALUE;
		for (Action a : set) { // Find the closest action value to the generated one.
			float candidate = (float) Math.abs(a.getTimestepsTotal() - rScaled);
			if (candidate < diff) {
				diff = candidate;
				best = a;
			}
		}
		return best;
	}

}
