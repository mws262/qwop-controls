package main;

public interface IActionGenerator {
	public Action[] getPotentialChildActionSet(Node parentNode);
	
}
