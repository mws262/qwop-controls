package game;

import game.action.Command;

/**
 * An external game must both be able to receive commands and provide a state. However, its timing and state might
 * not be externally changeable. Typically this will apply to the Flash game.
 *
 * @author matt
 */
public interface IGameExternal<C extends Command<?>> extends IGameCommandTarget<C>, IGameStateSource {}
