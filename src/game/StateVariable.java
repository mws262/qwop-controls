package game;

import java.io.Serializable;

public class StateVariable implements Serializable{

	private static final long serialVersionUID = 1L;
	
	public float x;
	public float y;
	public float th;
	public float dx;
	public float dy;
	public float dth;
	
	public StateVariable(float x, float y, float th, float dx, float dy, float dth){
		this.x = x;
		this.y = y;
		this.th = th;
		this.dx = dx;
		this.dy = dy;
		this.dth = dth;
	}	
}