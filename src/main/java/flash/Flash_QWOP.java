package flash;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import chrriis.common.UIUtils;
import chrriis.dj.nativeswing.swtimpl.NativeInterface;
import chrriis.dj.nativeswing.swtimpl.components.JFlashPlayer;
import chrriis.dj.nativeswing.swtimpl.components.JWebBrowser;
import chrriis.dj.nativeswing.swtimpl.components.WebBrowserFunction;
import controllers.Controller_NearestNeighborApprox;
import game.GameThreadSafe;
import game.State;
import actions.Action;
import actions.ActionQueue;
import controllers.IController;
import ui.PanelRunner_SimpleState;

public class Flash_QWOP extends JFrame {

    private static final long serialVersionUID = 1L;

    private PanelRunner_SimpleState runnerPane;
    private IController controller;

    /** **/
    private boolean Q = false;
    private boolean W = false;
    private boolean O = false;
    private boolean P = false;

    private boolean resetFlag = false;
    private State currentState;

    private float currentTime = 0f;
    private int timestep = 0;

    private ActionQueue actionQueue = new ActionQueue();

    private JComponent setupFlash() {
        JFlashPlayer flashPlayer = new JFlashPlayer();

        // Load flash QWOP.
        flashPlayer.load("src/main/resources/flash/athleticsFullState_twoDigits.swf");

        // Function calls from QWOP. Can only include the function calls which do no have return values.
        flashPlayer.addFlashPlayerListener(e -> {
            // receiveState
            //System.out.println(e.getCommand());
            switch (e.getCommand()) {
                case "getQWOPData":

                    String[] stateString = ((String) e.getParameters()[0]).split(",");
                    timestep = Float.valueOf(stateString[0].trim()).intValue();

                    float[] states = new float[State.ObjectName.values().length * State.StateName.values().length];
                    for (int i = 0; i < states.length; i++) {
                        states[i] = Float.valueOf(stateString[i + 1].trim());
                    }

                    currentState = new State(states, false);
                    GameThreadSafe.adjustRealQWOPStateToSimState(currentState);
                    actionQueue.addAction(controller.policy(currentState));

                    runnerPane.updateState(currentState);
                    break;

                case "flashTime":
                    currentTime = Float.valueOf((String) e.getParameters()[0]);
                    break;
            }
        });

        // The game asks this for the control inputs. Can't return values with the normal FlashPlayerListener.
        flashPlayer.getWebBrowser().registerFunction(new WebBrowserFunction("getQWOPControls") {
            public Object invoke(JWebBrowser webBrowser, Object... args) {

                if (!actionQueue.isEmpty()) {
                    boolean[] keys = actionQueue.pollCommand();
                    Q = keys[0];
                    W = keys[1];
                    O = keys[2];
                    P = keys[3];
                } else {
                    System.out.println("Warning: No updated command available to the Flash game in time. Using the " +
                            "previously set keys.");
                }

                int qNum = Q ? 1 : 0;
                int wNum = W ? 1 : 0;
                int oNum = O ? 1 : 0;
                int pNum = P ? 1 : 0;

                if (resetFlag) {
                    System.out.println("Flash game reset.");
                    resetFlag = false;
                    return (new Number[]{0, 0, 0, 0, 1, 0, 0});
                } else {
                    System.out.println(timestep + "," + Q + "," + W + "," + O + "," + P);
                    return (new Number[]{qNum, wNum, oNum, pNum, 0, 0, 0});
                }
            }
        });
        return flashPlayer;
    }

    private void flagForReset() {
        resetFlag = true;
    }

    private void setupUI() {
        NativeInterface.open();
        UIUtils.setPreferredLookAndFeel();
        SwingUtilities.invokeLater(() -> {
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            getContentPane().add(setupFlash(), BorderLayout.CENTER);
            getContentPane().setPreferredSize(new Dimension(1000, 1000));
            /* Runner pane */
            runnerPane = new PanelRunner_SimpleState();
            runnerPane.activateTab();
            runnerPane.setPreferredSize(new Dimension(1000, 400));
            add(runnerPane, BorderLayout.PAGE_END);
            Thread drawThread = new Thread(runnerPane);
            drawThread.start();

            setSize(1000, 2000);
            setLocationByPlatform(true);
            setVisible(true);
            pack();
        });

        NativeInterface.runEventPump();
    }

    private void setupController() {
        // CONTROLLER -- Approximate nearest neighbor.
        File saveLoc = new File("src/main/resources/saved_data/training_data");

        File[] allFiles = saveLoc.listFiles();
        if (allFiles == null) throw new RuntimeException("Bad directory given: " + saveLoc.getName());

        List<File> exampleDataFiles = new ArrayList<>();
        for (File f : allFiles) {
            if (f.getName().contains("TFRecord")) {
                System.out.println("Found save file: " + f.getName());
                exampleDataFiles.add(f);
            }
        }
        controller = new Controller_NearestNeighborApprox(exampleDataFiles);
    }

    /** Standard goals method to try that test as a standalone application. **/
    public static void main(String[] args) {
        Flash_QWOP gameQWOP = new Flash_QWOP();
        gameQWOP.actionQueue.addAction(new Action(8, false, false, false, false));
        gameQWOP.actionQueue.addAction(new Action(50, false, true, true, false));
        gameQWOP.actionQueue.addAction(new Action(1, false, false, false, false));
        gameQWOP.actionQueue.addAction(new Action(54, true, false, false, true));
        gameQWOP.flagForReset();
        gameQWOP.setupController();
        gameQWOP.setupUI();
    }
}
