package controllers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import game.IGameInternal;
import game.IGameSerializable;
import game.action.Action;
import game.action.Command;
import game.state.IState;
import tree.node.NodeGameExplorableBase;

import java.awt.*;

/**
 * Interface for defining general QWOP controllers. Follows the typical state to command mapping. If an implementation
 * of an IController wants to use a history of states or game.command, it should store these locally itself.
 *
 * @author matt
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Controller_Random.class, name = "random"),
        @JsonSubTypes.Type(value = Controller_Null.class, name = "null"),
        @JsonSubTypes.Type(value = Controller_ValueFunction.class, name = "value_function"),
//        @JsonSubTypes.Type(value = Controller_NearestNeighborApprox.class, name = "nearest_neighbor"),
        @JsonSubTypes.Type(value = Controller_Tensorflow_ClassifyActionsPerTimestep.class, name = "classifier")
})
public interface IController<C extends Command<?>, S extends IState> extends AutoCloseable {

    /**
     * Controller maps a current state to an command to take.
     *
     * @param state Current state.
     * @return An command to take.
     */
    Action<C> policy(NodeGameExplorableBase<?, C, S> state);

    /**
     * Get a control command. For some controllers, the hidden game state can be used in the policy. For this, an
     * IGameSerializable at the current game state may be used. For some controllers, this game parameter is ignored.
     * @param state Current visible state.
     * @param game Game at the current configuration containing the hidden state.
     * @return An command to take.
     */
    Action<C> policy(NodeGameExplorableBase<?, C, S> state, IGameSerializable<C, S> game);

    @JsonIgnore
    IController<C, S> getCopy();
    
    @Override
    void close();

    /**
     * Optionally, if we want the controller to draw anything to see what it's doing. Defaults to doing nothing if
     * not overridden.
     *
     * @param yOffsetPixels Vertical pixel offset from scaled world coordinates.
     * @param xOffsetPixels Horizontal pixel offset from scaled world coordinates.
     * @param runnerScaling Scaling from world coordinates to window pixel coordinates.
     **/
    default void draw(Graphics g, IGameInternal<C, S> game, float runnerScaling, int xOffsetPixels,
                      int yOffsetPixels) {}
}
