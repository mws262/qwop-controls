package goals.value_function;

import actions.Action;
import flashqwop.FlashGame;
import game.GameUnified;
import game.State;
import org.jblas.util.Random;
import tree.Node;
import value.ValueFunction_TensorFlow;
import value.ValueFunction_TensorFlow_StateOnly;
import vision.CaptureQWOPWindow;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.Queue;

// -XX:+UseShenandoahGC -XX:-UseBiasedLocking -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:ShenandoahMinFreeThreshold=10 -XX:+PrintGCDetails

@SuppressWarnings("Duplicates")
public class MAIN_FlashEvaluation extends FlashGame {

    private boolean imageCapture = false;
    private boolean addActionNoise = false;
    private float noiseProbability = 0.3f;
    private CaptureQWOPWindow capture;
    private File captureDir = new File("vision_capture");

    Action[] prefix = new Action[]{
            new Action(7, Action.Keys.none),
//            new Action(40, Action.Keys.wo),
//            new Action(20, Action.Keys.qp),
//            new Action(1, Action.Keys.p),
//            new Action(19, Action.Keys.qp),
//            new Action(3, Action.Keys.wo),

    };

    private ValueFunction_TensorFlow valueFunction = null;
    private Node placeholderNode = new Node(); // TODO only really needs the state. This is just acting as a container.

    public MAIN_FlashEvaluation() {
        if (imageCapture) {
            capture = new CaptureQWOPWindow(0); // Whichever screen has toolbar is 0.
            if (!captureDir.exists() || !captureDir.isDirectory()) {
                captureDir.mkdirs();
            }
        }

        loadController();

        getControlAction(GameUnified.getInitialState()); // TODO make this better. The first controller evaluation ever
        // takes 8 times longer than the rest. I don't know why. In the meantime, just do the first evaluation in a
        // non-time-critical section of the code. In the long term, the controller should be an anytime approach
        // anyway.
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        printGameInfo();
        restart();
    }

    @Override
    public Action[] getActionSequenceFromBeginning() {
        return prefix;
    }

    @Override
    public Action getControlAction(State state) {
        placeholderNode.setState(state);
        Action action = valueFunction.getMaximizingAction(placeholderNode);
        if (addActionNoise && Random.nextFloat() < noiseProbability) {
            if (action.getTimestepsTotal() < 2 || Random.nextFloat() > 0.5f) {
                action = new Action(action.getTimestepsTotal() + 1, action.peek());
            } else {
                action = new Action(action.getTimestepsTotal() - 1, action.peek());
            }
        }
        return action;
    }

    int runCounter = 0;
    File runFile;
    boolean resetPending = true;

    Queue<File> capturesThisRun = new LinkedList<>();
    Queue<State> statesThisRun = new LinkedList<>();

    @Override
    public void reportGameStatus(State state, boolean[] command, int timestep) {

        if (!imageCapture) return;

        // Failure detected for the first time.
        if (!resetPending && state.isFailed()) {
            String saveString = "";
            while (!capturesThisRun.isEmpty()) {
                File f = capturesThisRun.poll();
                saveString += f.getName() + '\t';
                State s = statesThisRun.poll();
                saveString += formatState(s) + "\r\n";
            }
            assert !statesThisRun.isEmpty();

            try {
                Files.write(new File(runFile.getPath() + "/poses.dat").toPath(), saveString.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }

            resetPending = true;
            return;
        }

        // Reset has just occurred.
        if (timestep == 0) {
            // Clear caches from the last game and make a new directory.
            capturesThisRun.clear();
            statesThisRun.clear();
            runFile = new File(captureDir.getPath() + "/run" + runCounter++);
            runFile.mkdirs();
            resetPending = false;
        } else if (resetPending) {
            return;
        }

        // Hold state info.
        statesThisRun.add(state);

        // Take picture.
        if (timestep > 0) {
            try {
                File nextCapture = new File(runFile.getPath() + "/ts" + (timestep - 1) + ".png");
                capture.saveImageToPNG(nextCapture);
                capturesThisRun.add(nextCapture);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void loadController() {
        // Load a value function controller.
        try {
            valueFunction = new ValueFunction_TensorFlow_StateOnly(new File("src/main/resources/tflow_models" +
                    "/small_net.pb")); // state_only.pb"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        valueFunction.loadCheckpoint("small566"); // "small289"); // _after439");//273");//chk_after1");
//        Node sampleNode = new Node();
//        for (int i = 0; i < 100; i++) {
//            valueFunction.evaluate(sampleNode);
//        }
    }

    public static void main(String[] args) {
        MAIN_FlashEvaluation controller = new MAIN_FlashEvaluation();
    }

    private String formatState(State s) {

        float bodyX = s.body.getX();

        String ss = s.body.getX() + "\t" + s.body.getY() + "\t" + s.body.getTh() + "\t";
        ss += (s.head.getX() - bodyX) + "\t" + s.head.getY() + "\t" + s.head.getTh() + "\t";
        ss += (s.rthigh.getX() - bodyX) + "\t" + s.rthigh.getY() + "\t" + s.rthigh.getTh() + "\t";
        ss += (s.lthigh.getX() - bodyX) + "\t" + s.lthigh.getY() + "\t" + s.lthigh.getTh() + "\t";
        ss += (s.rcalf.getX() - bodyX) + "\t" + s.rcalf.getY() + "\t" + s.rcalf.getTh() + "\t";
        ss += (s.lcalf.getX() - bodyX) + "\t" + s.lcalf.getY() + "\t" + s.lcalf.getTh() + "\t";
        ss += (s.rfoot.getX() - bodyX) + "\t" + s.rfoot.getY() + "\t" + s.rfoot.getTh() + "\t";
        ss += (s.lfoot.getX() - bodyX) + "\t" + s.lfoot.getY() + "\t" + s.lfoot.getTh() + "\t";
        ss += (s.ruarm.getX() - bodyX) + "\t" + s.ruarm.getY() + "\t" + s.ruarm.getTh() + "\t";
        ss += (s.luarm.getX() - bodyX) + "\t" + s.luarm.getY() + "\t" + s.luarm.getTh() + "\t";
        ss += (s.rlarm.getX() - bodyX) + "\t" + s.rlarm.getY() + "\t" + s.rlarm.getTh() + "\t";
        ss += (s.llarm.getX() - bodyX) + "\t" + s.llarm.getY() + "\t" + s.llarm.getTh() + "\t";

        ss += s.body.getDx() + "\t" + s.body.getDy() + "\t" + s.body.getDth() + "\t";
        ss += s.head.getDx() + "\t" + s.head.getDy() + "\t" + s.head.getDth() + "\t";
        ss += s.rthigh.getDx() + "\t" + s.rthigh.getDy() + "\t" + s.rthigh.getDth() + "\t";
        ss += s.lthigh.getDx()  + "\t" + s.lthigh.getDy() + "\t" + s.lthigh.getDth() + "\t";
        ss += s.rcalf.getDx() + "\t" + s.rcalf.getDy() + "\t" + s.rcalf.getDth() + "\t";
        ss += s.lcalf.getDx() + "\t" + s.lcalf.getDy() + "\t" + s.lcalf.getDth() + "\t";
        ss += s.rfoot.getDx() + "\t" + s.rfoot.getDy() + "\t" + s.rfoot.getDth() + "\t";
        ss += s.lfoot.getDx() + "\t" + s.lfoot.getDy() + "\t" + s.lfoot.getDth() + "\t";
        ss += s.ruarm.getDx() + "\t" + s.ruarm.getDy() + "\t" + s.ruarm.getDth() + "\t";
        ss += s.luarm.getDx() + "\t" + s.luarm.getDy() + "\t" + s.luarm.getDth() + "\t";
        ss += s.rlarm.getDx()+ "\t" + s.rlarm.getDy() + "\t" + s.rlarm.getDth() + "\t";
        ss += s.llarm.getDx() + "\t" + s.llarm.getDy() + "\t" + s.llarm.getDth() + "\t";
        return ss;
    }

}
