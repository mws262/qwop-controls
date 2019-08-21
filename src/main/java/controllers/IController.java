package controllers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import game.GameUnified;
import game.IGameSerializable;
import game.action.Action;
import tree.node.NodeQWOPExplorableBase;

import java.awt.*;

/**
 * Interface for defining general QWOP controllers. Follows the typical state to action mapping. If an implementation
 * of an IController wants to use a history of states or game.action, it should store these locally itself.
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
public interface IController extends AutoCloseable {

    /**
     * Controller maps a current state to an action to take.
     *
     * @param state Current state.
     * @return An action to take.
     */
    Action policy(NodeQWOPExplorableBase<?> state);

    /**
     * Get a control action. For some controllers, the hidden game state can be used in the policy. For this, an
     * IGameSerializable at the current game state may be used. For some controllers, this game parameter is ignored.
     * @param state Current visible state.
     * @param game Game at the current configuration containing the hidden state.
     * @return An action to take.
     */
    Action policy(NodeQWOPExplorableBase<?> state, IGameSerializable game);

    @JsonIgnore
    IController getCopy();
    
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
    default void draw(Graphics g, GameUnified game, float runnerScaling, int xOffsetPixels, int yOffsetPixels) {}
}
