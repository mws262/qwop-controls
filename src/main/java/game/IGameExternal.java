package game;

import game.action.Command;
import game.state.IState;

/**
 * An external game must both be able to receive commands and provide a state. However, its timing and state might
 * not be externally changeable. Typically this will apply to the Flash game.
 *
 * @author matt
 */
public interface IGameExternal<C extends Command<?>, S extends IState> extends IGameCommandTarget<C>,
        IGameStateSource<S> {}
