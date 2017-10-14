package main;

public interface IActionGenerator {
	public Action[] getPotentialChildActionSet(TrialNodeMinimal parentNode);
	
}
