package flash;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import chrriis.common.UIUtils;
import chrriis.dj.nativeswing.swtimpl.NativeInterface;
import chrriis.dj.nativeswing.swtimpl.components.FlashPlayerCommandEvent;
import chrriis.dj.nativeswing.swtimpl.components.FlashPlayerListener;
import chrriis.dj.nativeswing.swtimpl.components.JFlashPlayer;
import chrriis.dj.nativeswing.swtimpl.components.JWebBrowser;
import chrriis.dj.nativeswing.swtimpl.components.WebBrowserFunction;
import controllers.Controller_NearestNeighborApprox;
import game.GameLoader;
import game.State;
import main.Action;
import main.IController;
import main.Utility;
import ui.PanelRunner_SimpleState;

public class Flash_QWOP extends JFrame {

	private static final long serialVersionUID = 1L;

	PanelRunner_SimpleState runnerPane;
	IController controller;

	/** **/
	public boolean Q = false;
	public boolean W = false;
	public boolean O = false;
	public boolean P = false;

	public boolean resetFlag = false;
	public State currentState;

	public float currentTime = 0f;
	public int timestep = 0;

	public JComponent setupFlash() {
		JFlashPlayer flashPlayer = new JFlashPlayer();

		// Load flash QWOP.
		flashPlayer.load(Utility.getExcutionPath() + "./flash_files/athleticsFullState_twoDigits.swf");

		// Function calls from QWOP. Can only include the function calls which do no have return values.
		flashPlayer.addFlashPlayerListener(new FlashPlayerListener() {
			public void commandReceived(FlashPlayerCommandEvent e) {
				// receiveState
				//System.out.println(e.getCommand());
				switch(e.getCommand()) {
				case "getQWOPData":
					List<Float> stateValues = // Zombeast code -> csv string to list of actual flots
					Arrays.asList(((String)e.getParameters()[0]).split(","))
					.stream()
					.map(String :: trim)
					.map(s -> Float.valueOf(s))
					.collect(Collectors.toList());

					timestep = stateValues.get(0).intValue();
					float[] states = new float[State.ObjectName.values().length * State.StateName.values().length];

					for (int i = 0; i < states.length; i++) {
						states[i] = stateValues.get(i + 1);
					}
					currentState = new State(states);
					GameLoader.adjustRealQWOPStateToSimState(currentState);

					Action act = controller.policy(currentState);
					Q = act.peek()[0];
					W = act.peek()[1];
					O = act.peek()[2];
					P = act.peek()[3];
					
					runnerPane.updateState(currentState);
					break;

				case "flashTime":
					currentTime = Float.valueOf((String) e.getParameters()[0]);
					break;
				}
			}
		});

		// The game asks this for the control inputs. Can't return values with the normal FlashPlayerListener.
		flashPlayer.getWebBrowser().registerFunction(new WebBrowserFunction("getQWOPControls") {
			public Object invoke(JWebBrowser webBrowser, Object... args) {
				int qNum = Q ? 1 : 0;
				int wNum = W ? 1 : 0;
				int oNum = O ? 1 : 0;
				int pNum = P ? 1 : 0;
				int resetNum = resetFlag ? 1 : 0;

				return (Object)(new Number[] {qNum, wNum, oNum, pNum, resetNum, 0, 0});
			}
		});
		return flashPlayer;
	}

	public static void testPrint(FlashPlayerCommandEvent e) {
		StringBuilder sb = new StringBuilder();
		Object[] parameters = e.getParameters();
		for (Object obj : parameters) {
			sb.append((String)(obj));
		}
		System.out.println(sb.toString());
	}

	public void setupUI() {
		NativeInterface.open();
		UIUtils.setPreferredLookAndFeel();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				getContentPane().add(setupFlash(), BorderLayout.CENTER);
				getContentPane().setPreferredSize(new Dimension(1000,1000));
				/* Runner pane */   
				runnerPane = new PanelRunner_SimpleState();
				runnerPane.activateTab();
				runnerPane.setPreferredSize(new Dimension(1000,400));
				add(runnerPane, BorderLayout.PAGE_END);
				Thread drawThread = new Thread(runnerPane);
				drawThread.start();

				setSize(1000, 2000);
				setLocationByPlatform(true);
				setVisible(true);
				pack();
			}
		});

		NativeInterface.runEventPump();
	}

	public void setupController() {
		// CONTROLLER -- Approximate nearest neighbor.
		File saveLoc = new File(Utility.getExcutionPath() + "saved_data/training_data");

		File[] allFiles = saveLoc.listFiles();
		if (allFiles == null) throw new RuntimeException("Bad directory given: " + saveLoc.getName());

		List<File> exampleDataFiles = new ArrayList<File>();
		for (File f : allFiles){
			if (f.getName().contains("TFRecord")) {
				System.out.println("Found save file: " + f.getName());
				exampleDataFiles.add(f);
			}
		}
		controller = new Controller_NearestNeighborApprox(exampleDataFiles);
	}

	/* Standard main method to try that test as a standalone application. */
	public static void main(String[] args) {
		Flash_QWOP gameQWOP = new Flash_QWOP();

		gameQWOP.setupController();
		gameQWOP.setupUI();
	}
}
