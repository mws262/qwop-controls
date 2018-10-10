package main;

public interface IActionGenerator {
    ActionSet getPotentialChildActionSet(Node parentNode);
}
