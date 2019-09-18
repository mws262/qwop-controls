package game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import game.action.Command;

/**
 * Any interface which can receive QWOP game commands. This could be a simulation, the real Flash game, hardware, etc.
 *
 * @author matt
 */
public interface IGameCommandTarget<C extends Command<?>> {
    void command(C command);

    @JsonIgnore
    int getNumberOfChoices();
}
