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
//public class EvaluationFunction_SqDistFromOtherTest {
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
//            slist2[i] = i / 5f;
//            slist3[i] = i / 7f;
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
//        IEvaluationFunction efun = new EvaluationFunction_SqDistFromOther(node1);
//
//        Assert.assertEquals(invSqDist(node1.getState().flattenState(), node1.getState().flattenState()),
//                efun.getValue(node1), 1e-3);
//        Assert.assertEquals(invSqDist(node1.getState().flattenState(), node2.getState().flattenState()),
//                efun.getValue(node2), 1e-3);
//        Assert.assertEquals(invSqDist(node1.getState().flattenState(), node3.getState().flattenState()),
//                efun.getValue(node3), 1e-3);
//        Assert.assertEquals(invSqDist(node1.getState().flattenState(), node4.getState().flattenState()),
//                efun.getValue(node4), 1e-3);
//
//        efun = new EvaluationFunction_SqDistFromOther(node2);
//
//        Assert.assertEquals(invSqDist(node2.getState().flattenState(), node1.getState().flattenState()),
//                efun.getValue(node1), 1e-3);
//        Assert.assertEquals(invSqDist(node2.getState().flattenState(), node2.getState().flattenState()),
//                efun.getValue(node2), 1e-3);
//        Assert.assertEquals(invSqDist(node2.getState().flattenState(), node3.getState().flattenState()),
//                efun.getValue(node3), 1e-3);
//        Assert.assertEquals(invSqDist(node2.getState().flattenState(), node4.getState().flattenState()),
//                efun.getValue(node4), 1e-3);
//
//        efun = new EvaluationFunction_SqDistFromOther(node3);
//
//        Assert.assertEquals(invSqDist(node3.getState().flattenState(), node1.getState().flattenState()),
//                efun.getValue(node1), 1e-3);
//        Assert.assertEquals(invSqDist(node3.getState().flattenState(), node2.getState().flattenState()),
//                efun.getValue(node2), 1e-3);
//        Assert.assertEquals(invSqDist(node3.getState().flattenState(), node3.getState().flattenState()),
//                efun.getValue(node3), 1e-3);
//        Assert.assertEquals(invSqDist(node3.getState().flattenState(), node4.getState().flattenState()),
//                efun.getValue(node4), 1e-3);
//
//        efun = new EvaluationFunction_SqDistFromOther(node4);
//
//        Assert.assertEquals(invSqDist(node4.getState().flattenState(), node1.getState().flattenState()),
//                efun.getValue(node1), 1e-3);
//        Assert.assertEquals(invSqDist(node4.getState().flattenState(), node2.getState().flattenState()),
//                efun.getValue(node2), 1e-3);
//        Assert.assertEquals(invSqDist(node4.getState().flattenState(), node3.getState().flattenState()),
//                efun.getValue(node3), 1e-3);
//        Assert.assertEquals(invSqDist(node4.getState().flattenState(), node4.getState().flattenState()),
//                efun.getValue(node4), 1e-3);
//    }
//
//    @Test
//    public void getCopy() {
//        IEvaluationFunction efun = new EvaluationFunction_SqDistFromOther(node1);
//        IEvaluationFunction efuncpy = efun.getCopy();
//
//        Assert.assertEquals(invSqDist(node1.getState().flattenState(), node1.getState().flattenState()),
//                efuncpy.getValue(node1), 1e-3);
//        Assert.assertEquals(invSqDist(node1.getState().flattenState(), node2.getState().flattenState()),
//                efuncpy.getValue(node2), 1e-3);
//        Assert.assertEquals(invSqDist(node1.getState().flattenState(), node3.getState().flattenState()),
//                efuncpy.getValue(node3), 1e-3);
//        Assert.assertEquals(invSqDist(node1.getState().flattenState(), node4.getState().flattenState()),
//                efuncpy.getValue(node4), 1e-3);
//
//        efun = new EvaluationFunction_SqDistFromOther(node2);
//        efuncpy = efun.getCopy();
//
//        Assert.assertEquals(invSqDist(node2.getState().flattenState(), node1.getState().flattenState()),
//                efuncpy.getValue(node1), 1e-3);
//        Assert.assertEquals(invSqDist(node2.getState().flattenState(), node2.getState().flattenState()),
//                efuncpy.getValue(node2), 1e-3);
//        Assert.assertEquals(invSqDist(node2.getState().flattenState(), node3.getState().flattenState()),
//                efuncpy.getValue(node3), 1e-3);
//        Assert.assertEquals(invSqDist(node2.getState().flattenState(), node4.getState().flattenState()),
//                efuncpy.getValue(node4), 1e-3);
//
//        efun = new EvaluationFunction_SqDistFromOther(node3);
//        efuncpy = efun.getCopy();
//
//        Assert.assertEquals(invSqDist(node3.getState().flattenState(), node1.getState().flattenState()),
//                efuncpy.getValue(node1), 1e-3);
//        Assert.assertEquals(invSqDist(node3.getState().flattenState(), node2.getState().flattenState()),
//                efuncpy.getValue(node2), 1e-3);
//        Assert.assertEquals(invSqDist(node3.getState().flattenState(), node3.getState().flattenState()),
//                efuncpy.getValue(node3), 1e-3);
//        Assert.assertEquals(invSqDist(node3.getState().flattenState(), node4.getState().flattenState()),
//                efuncpy.getValue(node4), 1e-3);
//
//        efun = new EvaluationFunction_SqDistFromOther(node4);
//        efuncpy = efun.getCopy();
//
//        Assert.assertEquals(invSqDist(node4.getState().flattenState(), node1.getState().flattenState()),
//                efuncpy.getValue(node1), 1e-3);
//        Assert.assertEquals(invSqDist(node4.getState().flattenState(), node2.getState().flattenState()),
//                efuncpy.getValue(node2), 1e-3);
//        Assert.assertEquals(invSqDist(node4.getState().flattenState(), node3.getState().flattenState()),
//                efuncpy.getValue(node3), 1e-3);
//        Assert.assertEquals(invSqDist(node4.getState().flattenState(), node4.getState().flattenState()),
//                efuncpy.getValue(node4), 1e-3);
//    }
//
//    private float invSqDist(float[] f1, float[] f2) {
//        float sq = 0;
//        for (int i = 0; i < f1.length; i++) {
//            float diff = f2[i] - f1[i];
//            sq -= diff * diff;
//        }
//        return sq;
//    }
//}