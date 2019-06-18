package controllers;

import game.actions.Action;
import game.state.IState;

/**
 * A do-nothing placeholder controller. It always concludes that no keys should be pressed.
 *
 * @author matt
 */
public class Controller_Null implements IController {

    @Override
    public Action policy(IState state) {
        return new Action(1, false, false, false, false);
    }
}
