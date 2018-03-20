package game;

public class TempTester {

	public static void main(String[] args) {
		GameLoader gl = new GameLoader();
		try {
			gl.makeNewWorld();
			for (int i = 0; i < 50; i++) {
				gl.stepGame(true, false, false, true);
				System.out.println(gl.lKneeJ.getClass().getMethod("getJointAngle").invoke(gl.lKneeJ));
			}
		}catch(Exception e) {
			e.printStackTrace();
		}	
	}
}
