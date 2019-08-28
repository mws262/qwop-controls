package game.qwop;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import game.IGameInternal;
import game.state.StateVariable6D;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.io.Serializable;

/**
 * Container class for holding the configurations and velocities of the entire runner at a single instance in time.
 * Has x, y, th, dx, dy, dth for 12 bodies, meaning that one StateQWOP represents 72 state values.
 *
 * @author matt
 */
public class StateQWOP implements IStateQWOP, Serializable {

    public static final int STATE_SIZE = 72;
    public static final int STATEVARIABLE_COUNT = 12;

    private static final long serialVersionUID = 2L;

    /**
     * Does this state represent a fallen configuration?
     */
    private boolean failedState;

    /**
     * Objects which hold the x, y, theta, dx, dy, dtheta values for all body parts.
     */
    public final StateVariable6D body, rthigh, lthigh, rcalf, lcalf, rfoot, lfoot, ruarm, luarm, rlarm, llarm, head;

    /**
     * Array holding a StateVariable6D for each body part.
     */
    private final StateVariable6D[] stateVariables;

    /**
     * Make new state from list of ordered numbers. Most useful for interacting with neural network stuff. Number
     * order is essential.
     *
     * @param stateVars Array of state variable values. Order matches TensorFlow in/out
     *                  {@link game.state.transform.Transform_Autoencoder order}.
     * @param isFailed  Whether this state represents a fallen runner.
     */
    public StateQWOP(float[] stateVars, boolean isFailed) {
        if (stateVars.length != STATE_SIZE) {
            throw new IndexOutOfBoundsException("Given array is not the correct size for creating a StateQWOP. Given size" +
                    " was: " + stateVars.length);
        }
        body = new StateVariable6D(stateVars[0], stateVars[1], stateVars[2], stateVars[3], stateVars[4], stateVars[5]);
        head = new StateVariable6D(stateVars[6], stateVars[7], stateVars[8], stateVars[9], stateVars[10], stateVars[11]);
        rthigh = new StateVariable6D(stateVars[12], stateVars[13], stateVars[14], stateVars[15], stateVars[16],
                stateVars[17]);
        lthigh = new StateVariable6D(stateVars[18], stateVars[19], stateVars[20], stateVars[21], stateVars[22],
                stateVars[23]);
        rcalf = new StateVariable6D(stateVars[24], stateVars[25], stateVars[26], stateVars[27], stateVars[28],
                stateVars[29]);
        lcalf = new StateVariable6D(stateVars[30], stateVars[31], stateVars[32], stateVars[33], stateVars[34],
                stateVars[35]);
        rfoot = new StateVariable6D(stateVars[36], stateVars[37], stateVars[38], stateVars[39], stateVars[40],
                stateVars[41]);
        lfoot = new StateVariable6D(stateVars[42], stateVars[43], stateVars[44], stateVars[45], stateVars[46],
                stateVars[47]);
        ruarm = new StateVariable6D(stateVars[48], stateVars[49], stateVars[50], stateVars[51], stateVars[52],
                stateVars[53]);
        luarm = new StateVariable6D(stateVars[54], stateVars[55], stateVars[56], stateVars[57], stateVars[58],
                stateVars[59]);
        rlarm = new StateVariable6D(stateVars[60], stateVars[61], stateVars[62], stateVars[63], stateVars[64],
                stateVars[65]);
        llarm = new StateVariable6D(stateVars[66], stateVars[67], stateVars[68], stateVars[69], stateVars[70],
                stateVars[71]);

        stateVariables = new StateVariable6D[]{body, head, rthigh, lthigh, rcalf, lcalf,
                rfoot, lfoot, ruarm, luarm, rlarm, llarm};

        failedState = isFailed;
    }

    /**
     * Make new state from a list of StateVariables. This is now the default way that the GameThreadSafe does it. To make
     * a new StateQWOP from an existing game, the best bet is to call {@link IGameInternal#getCurrentState()}.
     *
     * @param bodyS    StateQWOP of the torso.
     * @param headS    StateQWOP of the head.
     * @param rthighS  StateQWOP of the right thigh.
     * @param lthighS  StateQWOP of the left thigh.
     * @param rcalfS   StateQWOP of the right shank.
     * @param lcalfS   StateQWOP of the left shank.
     * @param rfootS   StateQWOP of the right foot.
     * @param lfootS   StateQWOP of the left foot.
     * @param ruarmS   StateQWOP of the right upper arm.
     * @param luarmS   StateQWOP of the left upper arm.
     * @param rlarmS   StateQWOP of the right lower arm.
     * @param llarmS   StateQWOP of the left lower arm.
     * @param isFailed Whether this state represents a fallen runner.
     */
    public StateQWOP(@JsonProperty("body") StateVariable6D bodyS,
                     @JsonProperty("head") StateVariable6D headS,
                     @JsonProperty("rthigh") StateVariable6D rthighS,
                     @JsonProperty("lthigh") StateVariable6D lthighS,
                     @JsonProperty("rcalf") StateVariable6D rcalfS,
                     @JsonProperty("lcalf") StateVariable6D lcalfS,
                     @JsonProperty("rfoot") StateVariable6D rfootS,
                     @JsonProperty("lfoot") StateVariable6D lfootS,
                     @JsonProperty("ruarm") StateVariable6D ruarmS,
                     @JsonProperty("luarm") StateVariable6D luarmS,
                     @JsonProperty("rlarm") StateVariable6D rlarmS,
                     @JsonProperty("llarm") StateVariable6D llarmS,
                     @JsonProperty("failed") boolean isFailed) {
        body = bodyS;
        head = headS;
        rthigh = rthighS;
        lthigh = lthighS;
        rcalf = rcalfS;
        lcalf = lcalfS;
        rfoot = rfootS;
        lfoot = lfootS;
        ruarm = ruarmS;
        luarm = luarmS;
        rlarm = rlarmS;
        llarm = llarmS;
        failedState = isFailed;

        stateVariables = new StateVariable6D[]{body, head, rthigh, lthigh, rcalf, lcalf,
                rfoot, lfoot, ruarm, luarm, rlarm, llarm};
    }

    public StateQWOP(StateVariable6D[] stateVariables, boolean isFailed) {
        this(stateVariables[0], stateVariables[1], stateVariables[2], stateVariables[3], stateVariables[4],
                stateVariables[5] ,stateVariables[6], stateVariables[7], stateVariables[8], stateVariables[9],
                stateVariables[10], stateVariables[11], isFailed);
        if (stateVariables.length != STATEVARIABLE_COUNT) {
            throw new IndexOutOfBoundsException("Incorrect number of state variables given: " + stateVariables.length);
        }
    }

    /**
     * Get the whole array of state variables.
     *
     * @return Array containing a {@link StateVariable6D StateVariable6D} for each runner link.
     */
    @JsonIgnore
    @Override
    public StateVariable6D[] getAllStateVariables() {
        return stateVariables;
    }

    @JsonIgnore
    @Override
    public int getStateVariableCount() {
        return stateVariables.length;
    }

    @JsonIgnore
    @Override
    public float getCenterX() {
        return body.getX();
    }

    @Override
    public StateVariable6D getStateVariableFromName(ObjectName obj) {
        StateVariable6D st;
        switch (obj) {
            case BODY:
                st = body;
                break;
            case HEAD:
                st = head;
                break;
            case LCALF:
                st = lcalf;
                break;
            case LFOOT:
                st = lfoot;
                break;
            case LLARM:
                st = llarm;
                break;
            case LTHIGH:
                st = lthigh;
                break;
            case LUARM:
                st = luarm;
                break;
            case RCALF:
                st = rcalf;
                break;
            case RFOOT:
                st = rfoot;
                break;
            case RLARM:
                st = rlarm;
                break;
            case RTHIGH:
                st = rthigh;
                break;
            case RUARM:
                st = ruarm;
                break;
            default:
                throw new RuntimeException("Unknown object state queried.");
        }
        return st;
    }

    @Override
    public float[] flattenState() {
        return flattenState(body.getX());
    }

    // with arbitrary x offset
    public float[] flattenState(float bodyX) {
        float[] flatState = new float[STATE_SIZE];
        // Body
        flatState[0] = body.getX() - bodyX;
        flatState[1] = body.getY();
        flatState[2] = body.getTh();
        flatState[3] = body.getDx();
        flatState[4] = body.getDy();
        flatState[5] = body.getDth();

        // head
        flatState[6] = head.getX() - bodyX;
        flatState[7] = head.getY();
        flatState[8] = head.getTh();
        flatState[9] = head.getDx();
        flatState[10] = head.getDy();
        flatState[11] = head.getDth();

        // rthigh
        flatState[12] = rthigh.getX() - bodyX;
        flatState[13] = rthigh.getY();
        flatState[14] = rthigh.getTh();
        flatState[15] = rthigh.getDx();
        flatState[16] = rthigh.getDy();
        flatState[17] = rthigh.getDth();

        // lthigh
        flatState[18] = lthigh.getX() - bodyX;
        flatState[19] = lthigh.getY();
        flatState[20] = lthigh.getTh();
        flatState[21] = lthigh.getDx();
        flatState[22] = lthigh.getDy();
        flatState[23] = lthigh.getDth();

        // rcalf
        flatState[24] = rcalf.getX() - bodyX;
        flatState[25] = rcalf.getY();
        flatState[26] = rcalf.getTh();
        flatState[27] = rcalf.getDx();
        flatState[28] = rcalf.getDy();
        flatState[29] = rcalf.getDth();

        // lcalf
        flatState[30] = lcalf.getX() - bodyX;
        flatState[31] = lcalf.getY();
        flatState[32] = lcalf.getTh();
        flatState[33] = lcalf.getDx();
        flatState[34] = lcalf.getDy();
        flatState[35] = lcalf.getDth();

        // rfoot
        flatState[36] = rfoot.getX() - bodyX;
        flatState[37] = rfoot.getY();
        flatState[38] = rfoot.getTh();
        flatState[39] = rfoot.getDx();
        flatState[40] = rfoot.getDy();
        flatState[41] = rfoot.getDth();

        // lfoot
        flatState[42] = lfoot.getX() - bodyX;
        flatState[43] = lfoot.getY();
        flatState[44] = lfoot.getTh();
        flatState[45] = lfoot.getDx();
        flatState[46] = lfoot.getDy();
        flatState[47] = lfoot.getDth();

        // ruarm
        flatState[48] = ruarm.getX() - bodyX;
        flatState[49] = ruarm.getY();
        flatState[50] = ruarm.getTh();
        flatState[51] = ruarm.getDx();
        flatState[52] = ruarm.getDy();
        flatState[53] = ruarm.getDth();

        // luarm
        flatState[54] = luarm.getX() - bodyX;
        flatState[55] = luarm.getY();
        flatState[56] = luarm.getTh();
        flatState[57] = luarm.getDx();
        flatState[58] = luarm.getDy();
        flatState[59] = luarm.getDth();

        // rlarm
        flatState[60] = rlarm.getX() - bodyX;
        flatState[61] = rlarm.getY();
        flatState[62] = rlarm.getTh();
        flatState[63] = rlarm.getDx();
        flatState[64] = rlarm.getDy();
        flatState[65] = rlarm.getDth();

        // llarm
        flatState[66] = llarm.getX() - bodyX;
        flatState[67] = llarm.getY();
        flatState[68] = llarm.getTh();
        flatState[69] = llarm.getDx();
        flatState[70] = llarm.getDy();
        flatState[71] = llarm.getDth();

        return flatState;
    }

    /**
     * Get whether this state represents a failed runner configuration.
     *
     * @return Runner "fallen" status. True means failed. False means not failed.
     */
    @Override
    public synchronized boolean isFailed() {
        return failedState;
    }

    @Override
    public int getStateSize() {
        return STATE_SIZE;
    }

    /**
     * Get a tab-separated list of the states in String form. This takes the same order that
     * {@link StateQWOP#flattenState()} uses.
     * @return String containing all the state values on a line.
     */
    public String toFlatString() {
        float[] states = flattenState();
        StringBuilder sb = new StringBuilder();
        for (float f : states) {
            sb.append(f).append('\t');
        }
        return sb.toString();
    }

    // TODO ALL BELOW ARE INEFFICIENT.
    public StateQWOP add(StateQWOP s) {
        StateVariable6D[] otherStateVars = s.getAllStateVariables();
        StateVariable6D[] resultStates = new StateVariable6D[stateVariables.length];
        for (int i = 0; i < stateVariables.length; i++) {
            resultStates[i] = stateVariables[i].add(otherStateVars[i]);
        }
        return new StateQWOP(resultStates, this.failedState || s.failedState);
    }

    public StateQWOP subtract(StateQWOP s) {
        StateVariable6D[] otherStateVars = s.getAllStateVariables();
        StateVariable6D[] resultStates = new StateVariable6D[stateVariables.length];
        for (int i = 0; i < stateVariables.length; i++) {
            resultStates[i] = stateVariables[i].subtract(otherStateVars[i]);
        }
        return new StateQWOP(resultStates, this.failedState || s.failedState);
    }

    public StateQWOP multiply(StateQWOP s) {
        StateVariable6D[] otherStateVars = s.getAllStateVariables();
        StateVariable6D[] resultStates = new StateVariable6D[stateVariables.length];
        for (int i = 0; i < stateVariables.length; i++) {
            resultStates[i] = stateVariables[i].multiply(otherStateVars[i]);
        }
        return new StateQWOP(resultStates, this.failedState || s.failedState);
    }

    public StateQWOP divide(StateQWOP s) {
        StateVariable6D[] otherStateVars = s.getAllStateVariables();
        StateVariable6D[] resultStates = new StateVariable6D[stateVariables.length];
        for (int i = 0; i < stateVariables.length; i++) {
            resultStates[i] = stateVariables[i].divide(otherStateVars[i]);
        }
        return new StateQWOP(resultStates, this.failedState || s.failedState);
    }

    // Scalar multiplication.
    public StateQWOP multiply(float multiplier) {
        StateVariable6D[] resultStates = new StateVariable6D[stateVariables.length];
        StateVariable6D svMultiplier = new StateVariable6D(multiplier, multiplier, multiplier, multiplier, multiplier, multiplier);
        for (int i = 0; i < stateVariables.length; i++) {
            resultStates[i] = stateVariables[i].multiply(svMultiplier);
        }
        return new StateQWOP(resultStates, this.failedState);
    }

    public float[] extractPositions(float xOffset) {
        float[] sflat = new float[STATE_SIZE/2];
        int idx = 0;
        for (StateVariable6D sVar : stateVariables) {
            sflat[idx++] = sVar.getX() - xOffset;
            sflat[idx++] = sVar.getY();
            sflat[idx++] = sVar.getTh();
        }
        return sflat;
    }

    public float[] extractPositions() {
        return extractPositions(0f);
    }

    public float[] extractVelocities() {
        float[] vflat = new float[STATE_SIZE/2];
        int idx = 0;
        for (StateVariable6D sVar : stateVariables) {
            vflat[idx++] = sVar.getDx();
            vflat[idx++] = sVar.getDy();
            vflat[idx++] = sVar.getDth();
        }
        return vflat;
    }

    public StateQWOP swapPosAndVel() {
        float[] flat = new float[STATE_SIZE];
        int idx = 0;
        for (StateVariable6D sVar : stateVariables) {
            flat[idx++] = sVar.getDx();
            flat[idx++] = sVar.getDy();
            flat[idx++] = sVar.getDth();
            flat[idx++] = sVar.getX();
            flat[idx++] = sVar.getY();
            flat[idx++] = sVar.getTh();
        }
        return new StateQWOP(flat, isFailed());
    }

    public StateQWOP xOffsetSubtract(float xOffset) {
        float[] sflat = new float[STATE_SIZE];
        int idx = 0;
        for (StateVariable6D sVar : stateVariables) {
            sflat[idx++] = sVar.getX() - xOffset;
            sflat[idx++] = sVar.getY();
            sflat[idx++] = sVar.getTh();
            sflat[idx++] = sVar.getDx();
            sflat[idx++] = sVar.getDy();
            sflat[idx++] = sVar.getDth();
        }
        return new StateQWOP(sflat, isFailed());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || (obj.getClass() != this.getClass())) {
            return false;
        }
        StateQWOP other = (StateQWOP) obj;

        EqualsBuilder equalsBuilder = new EqualsBuilder();
        StateVariable6D[] stThis = this.getAllStateVariables();
        StateVariable6D[] stOther = other.getAllStateVariables();

        for (int i = 0; i < stThis.length; i++) {
            equalsBuilder.append(stThis[i], stOther[i]);
        }
        equalsBuilder.append(this.isFailed(), other.isFailed());

        return equalsBuilder.isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        for (StateVariable6D sv : stateVariables) {
            hashCodeBuilder.append(sv);
        }

        hashCodeBuilder.append(failedState);
        return hashCodeBuilder.toHashCode();
    }
}

