//package evaluators;
//
//import actions.Action;
//import game.State;
//import org.junit.Assert;
//import org.junit.Before;
//import org.junit.Test;
//import tree.Node;
//
//import static org.mockito.Mockito.mock;
//
//@SuppressWarnings("Duplicates")
//public class EvaluationFunction_ConstantTest {
//
//    // Some sample actions (mocked).
//    private Action a1 = mock(Action.class);
//    private Action a2 = mock(Action.class);
//    private Action a3 = mock(Action.class);
//
//    private Node node1;
//    private Node node2;
//    private Node node3;
//    private Node node4;
//
//    @Before
//    public void setup() {
//        node1 = new Node();
//        node2 = node1.addChild(a1);
//        node3 = node1.addChild(a2);
//        node4 = node2.addChild(a3);
//
//        float[] slist1 = new float[72];
//        float[] slist2 = new float[72];
//        float[] slist3 = new float[72];
//
//        for (int i = 0; i < 72; i++) {
//            slist1[i] = i / 3f;
//            slist2[i] = i * 2f;
//            slist3[i] = i * 7f;
//        }
//
//        State s1 = new State(slist1, false);
//        State s2 = new State(slist2, false);
//        State s3 = new State(slist3, false);
//
//        node2.setState(s1);
//        node3.setState(s2);
//        node4.setState(s3);
//    }
//
//    @Test
//    public void getValue() {
//        IEvaluationFunction efun = new EvaluationFunction_Constant(4.2f);
//        Assert.assertEquals(4.2f, efun.getValue(node1), 1e-10);
//        Assert.assertEquals(4.2f, efun.getValue(node2), 1e-10);
//        Assert.assertEquals(4.2f, efun.getValue(node3), 1e-10);
//        Assert.assertEquals(4.2f, efun.getValue(node4), 1e-10);
//
//        efun = new EvaluationFunction_Constant(-0.2f);
//        Assert.assertEquals(-0.2f, efun.getValue(node1), 1e-10);
//        Assert.assertEquals(-0.2f, efun.getValue(node2), 1e-10);
//        Assert.assertEquals(-0.2f, efun.getValue(node3), 1e-10);
//        Assert.assertEquals(-0.2f, efun.getValue(node4), 1e-10);
//    }
//
//    @Test
//    public void getValueString() {
//        IEvaluationFunction efun = new EvaluationFunction_Constant(4.2f);
//        Assert.assertEquals(String.valueOf(4.2f), efun.getValueString(node1));
//        Assert.assertEquals(String.valueOf(4.2f), efun.getValueString(node2));
//        Assert.assertEquals(String.valueOf(4.2f), efun.getValueString(node3));
//        Assert.assertEquals(String.valueOf(4.2f), efun.getValueString(node4));
//
//        efun = new EvaluationFunction_Constant(-0.2f);
//        Assert.assertEquals(String.valueOf(-0.2f), efun.getValueString(node1));
//        Assert.assertEquals(String.valueOf(-0.2f), efun.getValueString(node2));
//        Assert.assertEquals(String.valueOf(-0.2f), efun.getValueString(node3));
//        Assert.assertEquals(String.valueOf(-0.2f), efun.getValueString(node4));
//    }
//
//    @Test
//    public void getCopy() {
//        IEvaluationFunction efun = new EvaluationFunction_Constant(4.2f);
//        IEvaluationFunction efuncpy = efun.getCopy();
//
//        Assert.assertEquals(4.2f, efuncpy.getValue(node1), 1e-10);
//        Assert.assertEquals(4.2f, efuncpy.getValue(node2), 1e-10);
//        Assert.assertEquals(4.2f, efuncpy.getValue(node3), 1e-10);
//        Assert.assertEquals(4.2f, efuncpy.getValue(node4), 1e-10);
//
//        efun = new EvaluationFunction_Constant(-0.2f);
//        efuncpy = efun.getCopy();
//
//        Assert.assertEquals(-0.2f, efuncpy.getValue(node1), 1e-10);
//        Assert.assertEquals(-0.2f, efuncpy.getValue(node2), 1e-10);
//        Assert.assertEquals(-0.2f, efuncpy.getValue(node3), 1e-10);
//        Assert.assertEquals(-0.2f, efuncpy.getValue(node4), 1e-10);
//    }
//}