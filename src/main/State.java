package main;
import java.io.Serializable;

import org.jbox2d.common.XForm;
import org.jbox2d.dynamics.Body;


public class State implements Serializable {

	private static final long serialVersionUID = 2L;
	
	public boolean failedState;
	
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
	
	public State(QWOPGame world) {
		Body nextBody = world.TorsoBody;
		body = new StateVariable(nextBody.getPosition().x,nextBody.getPosition().y,nextBody.getAngle(),
				nextBody.getLinearVelocity().x,nextBody.getLinearVelocity().y,nextBody.getAngularVelocity());
		nextBody = world.RThighBody;
		rthigh = new StateVariable(nextBody.getPosition().x,nextBody.getPosition().y,nextBody.getAngle(),
				nextBody.getLinearVelocity().x,nextBody.getLinearVelocity().y,nextBody.getAngularVelocity());
		nextBody = world.LThighBody;
		lthigh = new StateVariable(nextBody.getPosition().x,nextBody.getPosition().y,nextBody.getAngle(),
				nextBody.getLinearVelocity().x,nextBody.getLinearVelocity().y,nextBody.getAngularVelocity());
		nextBody = world.RCalfBody;
		rcalf = new StateVariable(nextBody.getPosition().x,nextBody.getPosition().y,nextBody.getAngle(),
				nextBody.getLinearVelocity().x,nextBody.getLinearVelocity().y,nextBody.getAngularVelocity());
		nextBody = world.LCalfBody;
		lcalf = new StateVariable(nextBody.getPosition().x,nextBody.getPosition().y,nextBody.getAngle(),
				nextBody.getLinearVelocity().x,nextBody.getLinearVelocity().y,nextBody.getAngularVelocity());
		nextBody = world.RFootBody;
		rfoot = new StateVariable(nextBody.getPosition().x,nextBody.getPosition().y,nextBody.getAngle(),
				nextBody.getLinearVelocity().x,nextBody.getLinearVelocity().y,nextBody.getAngularVelocity());
		nextBody = world.LFootBody;
		lfoot = new StateVariable(nextBody.getPosition().x,nextBody.getPosition().y,nextBody.getAngle(),
				nextBody.getLinearVelocity().x,nextBody.getLinearVelocity().y,nextBody.getAngularVelocity());
		nextBody = world.RUArmBody;
		ruarm = new StateVariable(nextBody.getPosition().x,nextBody.getPosition().y,nextBody.getAngle(),
				nextBody.getLinearVelocity().x,nextBody.getLinearVelocity().y,nextBody.getAngularVelocity());
		nextBody = world.LUArmBody;
		luarm = new StateVariable(nextBody.getPosition().x,nextBody.getPosition().y,nextBody.getAngle(),
				nextBody.getLinearVelocity().x,nextBody.getLinearVelocity().y,nextBody.getAngularVelocity());
		nextBody = world.RLArmBody;
		rlarm = new StateVariable(nextBody.getPosition().x,nextBody.getPosition().y,nextBody.getAngle(),
				nextBody.getLinearVelocity().x,nextBody.getLinearVelocity().y,nextBody.getAngularVelocity());
		nextBody = world.LLArmBody;
		llarm = new StateVariable(nextBody.getPosition().x,nextBody.getPosition().y,nextBody.getAngle(),
				nextBody.getLinearVelocity().x,nextBody.getLinearVelocity().y,nextBody.getAngularVelocity());
		nextBody = world.HeadBody;
		head = new StateVariable(nextBody.getPosition().x,nextBody.getPosition().y,nextBody.getAngle(),
				nextBody.getLinearVelocity().x,nextBody.getLinearVelocity().y,nextBody.getAngularVelocity());
	}
	
	public XForm[] getTransforms(){
		XForm[] transforms = new XForm[13];

		//TODO check offsets and if not used, condense this code.
		// Body
		transforms[0] = new XForm();
		transforms[0].position.x = body.x;
		transforms[0].position.y = body.y;
		transforms[0].R.set(body.th);
		
		transforms[1] = new XForm();
		transforms[1].position.x = head.x;
		transforms[1].position.y = head.y;
		transforms[1].R.set(head.th);
		
		transforms[2] = new XForm();
		transforms[2].position.x = rfoot.x;
		transforms[2].position.y = rfoot.y;
		transforms[2].R.set(rfoot.th);
		
		transforms[3] = new XForm();
		transforms[3].position.x = lfoot.x;
		transforms[3].position.y = lfoot.y;
		transforms[3].R.set(lfoot.th);
		
		transforms[4] = new XForm();
		transforms[4].position.x = rcalf.x;
		transforms[4].position.y = rcalf.y;
		transforms[4].R.set(rcalf.th);
		
		transforms[5] = new XForm();
		transforms[5].position.x = lcalf.x;
		transforms[5].position.y = lcalf.y;
		transforms[5].R.set(lcalf.th);
		
		transforms[6] = new XForm();
		transforms[6].position.x = rthigh.x;
		transforms[6].position.y = rthigh.y;
		transforms[6].R.set(rthigh.th);
		
		transforms[7] = new XForm();
		transforms[7].position.x = lthigh.x;
		transforms[7].position.y = lthigh.y;
		transforms[7].R.set(lthigh.th);
		
		transforms[8] = new XForm();
		transforms[8].position.x = ruarm.x;
		transforms[8].position.y = ruarm.y;
		transforms[8].R.set(ruarm.th);
		
		transforms[9] = new XForm();
		transforms[9].position.x = luarm.x;
		transforms[9].position.y = luarm.y;
		transforms[9].R.set(luarm.th);
		
		transforms[10] = new XForm();
		transforms[10].position.x = rlarm.x;
		transforms[10].position.y = rlarm.y;
		transforms[10].R.set(rlarm.th);
		
		transforms[11] = new XForm();
		transforms[11].position.x = llarm.x;
		transforms[11].position.y = llarm.y;
		transforms[11].R.set(llarm.th);
		
		transforms[12] = new XForm(); // Cheating on the track for the time being.
		transforms[12].position.x = 0;
		transforms[12].position.y = 28.90813f;
		transforms[12].R.set(0);
		
		return transforms;
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

	
	public class StateVariable implements Serializable{

		private static final long serialVersionUID = 1L;
		
		public float x;
		public float y;
		public float th;
		public float dx;
		public float dy;
		public float dth;
		
		private StateVariable(float x, float y, float th, float dx, float dy, float dth){
			this.x = x;
			this.y = y;
			this.th = th;
			this.dx = dx;
			this.dy = dy;
			this.dth = dth;
		}	
	}
}

