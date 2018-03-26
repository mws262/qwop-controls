package main;
import java.io.Serializable;

public class State implements Serializable {

	private static final long serialVersionUID = 2L;
	
	public boolean failedState = false;
	
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
	
	public enum ObjectName{
		BODY, HEAD, RTHIGH, LTHIGH, RCALF, LCALF, RFOOT, LFOOT, RUARM, LUARM, RLARM, LLARM
	}
	
	public enum StateName{
		X, Y, TH, DX, DY, DTH
	}

	/** Make new state from list of ordered numbers. Most useful for interacting with neural network stuff. Number order is essential. **/
	public State(float[] stateVars) { // Order matches order in TensorflosAutoencoder.java
		body = new StateVariable(stateVars[0], stateVars[1], stateVars[2], stateVars[3], stateVars[4], stateVars[5]);
		head = new StateVariable(stateVars[6], stateVars[7], stateVars[8], stateVars[9], stateVars[10], stateVars[11]);
		rthigh = new StateVariable(stateVars[12], stateVars[13], stateVars[14], stateVars[15], stateVars[16], stateVars[17]);
		lthigh = new StateVariable(stateVars[18], stateVars[19], stateVars[20], stateVars[21], stateVars[22], stateVars[23]);
		rcalf = new StateVariable(stateVars[24], stateVars[25], stateVars[26], stateVars[27], stateVars[28], stateVars[29]);
		lcalf = new StateVariable(stateVars[30], stateVars[31], stateVars[32], stateVars[33], stateVars[34], stateVars[35]);
		rfoot = new StateVariable(stateVars[36], stateVars[37], stateVars[38], stateVars[39], stateVars[40], stateVars[41]);
		lfoot = new StateVariable(stateVars[42], stateVars[43], stateVars[44], stateVars[45], stateVars[46], stateVars[47]);
		ruarm = new StateVariable(stateVars[48], stateVars[49], stateVars[50], stateVars[51], stateVars[52], stateVars[53]);
		luarm = new StateVariable(stateVars[54], stateVars[55], stateVars[56], stateVars[57], stateVars[58], stateVars[59]);
		rlarm = new StateVariable(stateVars[60], stateVars[61], stateVars[62], stateVars[63], stateVars[64], stateVars[65]);
		llarm = new StateVariable(stateVars[66], stateVars[67], stateVars[68], stateVars[69], stateVars[70], stateVars[71]);
	}
	
	/** Make new state from a list of StateVariables. This is now the default way that the GameLoader does it. To make a new State from an existing game,
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
	public State(StateVariable bodyS, StateVariable headS, StateVariable rthighS, StateVariable lthighS, StateVariable rcalfS, StateVariable lcalfS,
			StateVariable rfootS, StateVariable lfootS, StateVariable ruarmS, StateVariable luarmS, StateVariable rlarmS, StateVariable llarmS, boolean isFailed) { // Order matches order in TensorflosAutoencoder.java
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
	}
	
	/** Get the value of the state you want using their names. I'll bet hashmaps do this better. **/
	public float getStateVarFromName(ObjectName obj, StateName state){
		StateVariable st;
		switch(obj){
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
		switch(state){
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
}

