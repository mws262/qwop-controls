package game;

/**
 * Any interface which can receive QWOP game commands. This could be a simulation, the real Flash game, hardware, etc.
 *
 * @author matt
 */
public interface IGameCommandTarget {

    /**
     * Send QWOP keypresses.
     * @param q Whether q is pressed on the keyboard (true is pressed down).
     * @param w Whether w is pressed on the keyboard (true is pressed down).
     * @param o Whether o is pressed on the keyboard (true is pressed down).
     * @param p Whether p is pressed on the keyboard (true is pressed down).
     */
    void command(boolean q, boolean w, boolean o, boolean p);

    /**
     * Send a QWOP command.
     * @param commands Array of keypress commands apply.
     */
    void command(boolean[] commands);

}
