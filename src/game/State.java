package game;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class State implements Serializable {

    private static final long serialVersionUID = 2L;

    private boolean failedState = false;

    public StateVariable body;
    public StateVariable rthigh;
    public StateVariable lthigh;
    public StateVariable rcalf;
    public StateVariable lcalf;
    public StateVariable rfoot;
    public StateVariable lfoot;
    public StateVariable ruarm;
    public StateVariable luarm;
    public StateVariable rlarm;
    public StateVariable llarm;
    public StateVariable head;

    private List<StateVariable> stateVariableList;

    public enum ObjectName {
        BODY, HEAD, RTHIGH, LTHIGH, RCALF, LCALF, RFOOT, LFOOT, RUARM, LUARM, RLARM, LLARM
    }

    public enum StateName {
        X, Y, TH, DX, DY, DTH
    }

    /**
     * Make new state from list of ordered numbers. Most useful for interacting with neural network stuff. Number
     * order is essential.
     **/
    public State(float[] stateVars) { // Order matches order in TensorflowAutoencoder.java
        body = new StateVariable(stateVars[0], stateVars[1], stateVars[2], stateVars[3], stateVars[4], stateVars[5]);
        head = new StateVariable(stateVars[6], stateVars[7], stateVars[8], stateVars[9], stateVars[10], stateVars[11]);
        rthigh = new StateVariable(stateVars[12], stateVars[13], stateVars[14], stateVars[15], stateVars[16],
				stateVars[17]);
        lthigh = new StateVariable(stateVars[18], stateVars[19], stateVars[20], stateVars[21], stateVars[22],
				stateVars[23]);
        rcalf = new StateVariable(stateVars[24], stateVars[25], stateVars[26], stateVars[27], stateVars[28],
				stateVars[29]);
        lcalf = new StateVariable(stateVars[30], stateVars[31], stateVars[32], stateVars[33], stateVars[34],
				stateVars[35]);
        rfoot = new StateVariable(stateVars[36], stateVars[37], stateVars[38], stateVars[39], stateVars[40],
				stateVars[41]);
        lfoot = new StateVariable(stateVars[42], stateVars[43], stateVars[44], stateVars[45], stateVars[46],
				stateVars[47]);
        ruarm = new StateVariable(stateVars[48], stateVars[49], stateVars[50], stateVars[51], stateVars[52],
				stateVars[53]);
        luarm = new StateVariable(stateVars[54], stateVars[55], stateVars[56], stateVars[57], stateVars[58],
				stateVars[59]);
        rlarm = new StateVariable(stateVars[60], stateVars[61], stateVars[62], stateVars[63], stateVars[64],
				stateVars[65]);
        llarm = new StateVariable(stateVars[66], stateVars[67], stateVars[68], stateVars[69], stateVars[70],
				stateVars[71]);

        stateVariableList = Arrays.asList(body, rthigh, lthigh, rcalf, lcalf,
                rfoot, lfoot, ruarm, luarm, rlarm, llarm, head);

    }

    /**
     * Make new state from a list of StateVariables. This is now the default way that the GameLoader does it. To make
     * a new State from an existing game,
     * the best bet is to call myGameLoader.getCurrentState().
     *
     * @param bodyS
     * @param headS
     * @param rthighS
     * @param lthighS
     * @param rcalfS
     * @param lcalfS
     * @param rfootS
     * @param lfootS
     * @param ruarmS
     * @param luarmS
     * @param rlarmS
     * @param llarmS
     */
    public State(StateVariable bodyS, StateVariable headS, StateVariable rthighS, StateVariable lthighS,
				 StateVariable rcalfS, StateVariable lcalfS,
                 StateVariable rfootS, StateVariable lfootS, StateVariable ruarmS, StateVariable luarmS,
				 StateVariable rlarmS, StateVariable llarmS, boolean isFailed) { // Order matches order in
    	// TensorflosAutoencoder.java
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

        stateVariableList = Arrays.asList(body, rthigh, lthigh, rcalf, lcalf,
                rfoot, lfoot, ruarm, luarm, rlarm, llarm, head);
    }

    /**
     * Get the whole list of state variables.
     **/
    public List<StateVariable> getStateList() {
        return stateVariableList;
    }

    /**
     * Get the value of the state you want using their names. I'll bet hashmaps do this better.
     **/
    public float getStateVarFromName(ObjectName obj, StateName state) {
        StateVariable st;
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
        float stateValue;
        switch (state) {
            case DTH:
                stateValue = st.dth;
                break;
            case DX:
                stateValue = st.dx;
                break;
            case DY:
                stateValue = st.dy;
                break;
            case TH:
                stateValue = st.th;
                break;
            case X:
                stateValue = st.x;
                break;
            case Y:
                stateValue = st.y;
                break;
            default:
                throw new RuntimeException("Unknown object state queried.");
        }
        return stateValue;
    }

    /**
     * Turn the state into an array of floats with body x subtracted from all x coordinates.
     **/
    public float[] flattenState() {
        float[] flatState = new float[72];
        float bodyX = body.x;

        // Body
        flatState[0] = 0;
        flatState[1] = body.y;
        flatState[2] = body.th;
        flatState[3] = body.dx;
        flatState[4] = body.dy;
        flatState[5] = body.dth;

        // head
        flatState[6] = head.x - bodyX;
        flatState[7] = head.y;
        flatState[8] = head.th;
        flatState[9] = head.dx;
        flatState[10] = head.dy;
        flatState[11] = head.dth;

        // rthigh
        flatState[12] = rthigh.x - bodyX;
        flatState[13] = rthigh.y;
        flatState[14] = rthigh.th;
        flatState[15] = rthigh.dx;
        flatState[16] = rthigh.dy;
        flatState[17] = rthigh.dth;

        // lthigh
        flatState[18] = lthigh.x - bodyX;
        flatState[19] = lthigh.y;
        flatState[20] = lthigh.th;
        flatState[21] = lthigh.dx;
        flatState[22] = lthigh.dy;
        flatState[23] = lthigh.dth;

        // rcalf
        flatState[24] = rcalf.x - bodyX;
        flatState[25] = rcalf.y;
        flatState[26] = rcalf.th;
        flatState[27] = rcalf.dx;
        flatState[28] = rcalf.dy;
        flatState[29] = rcalf.dth;

        // lcalf
        flatState[30] = lcalf.x - bodyX;
        flatState[31] = lcalf.y;
        flatState[32] = lcalf.th;
        flatState[33] = lcalf.dx;
        flatState[34] = lcalf.dy;
        flatState[35] = lcalf.dth;

        // rfoot
        flatState[36] = rfoot.x - bodyX;
        flatState[37] = rfoot.y;
        flatState[38] = rfoot.th;
        flatState[39] = rfoot.dx;
        flatState[40] = rfoot.dy;
        flatState[41] = rfoot.dth;

        // lfoot
        flatState[42] = lfoot.x - bodyX;
        flatState[43] = lfoot.y;
        flatState[44] = lfoot.th;
        flatState[45] = lfoot.dx;
        flatState[46] = lfoot.dy;
        flatState[47] = lfoot.dth;

        // ruarm
        flatState[48] = ruarm.x - bodyX;
        flatState[49] = ruarm.y;
        flatState[50] = ruarm.th;
        flatState[51] = ruarm.dx;
        flatState[52] = ruarm.dy;
        flatState[53] = ruarm.dth;

        // luarm
        flatState[54] = luarm.x - bodyX;
        flatState[55] = luarm.y;
        flatState[56] = luarm.th;
        flatState[57] = luarm.dx;
        flatState[58] = luarm.dy;
        flatState[59] = luarm.dth;
        // rlarm
        flatState[60] = rlarm.x - bodyX;
        flatState[61] = rlarm.y;
        flatState[62] = rlarm.th;
        flatState[63] = rlarm.dx;
        flatState[64] = rlarm.dy;
        flatState[65] = rlarm.dth;

        // llarm
        flatState[66] = llarm.x - bodyX;
        flatState[67] = llarm.y;
        flatState[68] = llarm.th;
        flatState[69] = llarm.dx;
        flatState[70] = llarm.dy;
        flatState[71] = llarm.dth;

        return flatState;
    }

    public synchronized boolean isFailed() {
        return failedState;
    }
}

