package tree.stage;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class TreeStage_GroupingTest {

    @Test
    public void groupingBehaviorTest() {
        TreeStage_Dummy dummyStage1 = new TreeStage_Dummy();
        TreeStage_Dummy dummyStage2 = new TreeStage_Dummy();
        TreeStage_Dummy dummyStage3 = new TreeStage_Dummy();
        TreeStage_Dummy dummyStage4 = new TreeStage_Dummy();

        TreeStage[] stages = new TreeStage[]{dummyStage1, dummyStage2, dummyStage3, dummyStage4};

        TreeStage_Grouping stageGrouping = new TreeStage_Grouping(stages);

        Assert.assertFalse(dummyStage1.isInitialized);
        Assert.assertEquals(dummyStage1, stageGrouping.getActiveStage());
        stageGrouping.initialize(null, null);
        Assert.assertTrue(dummyStage1.isInitialized);
        Assert.assertFalse(dummyStage2.isInitialized);

        Assert.assertEquals(dummyStage1, stageGrouping.getActiveStage());
        Assert.assertFalse(stageGrouping.checkTerminationConditions());
        dummyStage1.terminate = true;
        Assert.assertTrue(dummyStage1.checkTerminationConditions());
        Assert.assertFalse(stageGrouping.checkTerminationConditions());

        Assert.assertTrue(dummyStage2.isInitialized);
        Assert.assertFalse(dummyStage3.isInitialized);
        Assert.assertEquals(dummyStage2, stageGrouping.getActiveStage());
        dummyStage2.terminate = true;
        Assert.assertTrue(dummyStage2.checkTerminationConditions());
        Assert.assertFalse(stageGrouping.checkTerminationConditions());

        Assert.assertTrue(dummyStage3.isInitialized);
        Assert.assertFalse(dummyStage4.isInitialized);
        Assert.assertEquals(dummyStage3, stageGrouping.getActiveStage());
        dummyStage3.terminate = true;
        Assert.assertTrue(dummyStage3.checkTerminationConditions());
        Assert.assertFalse(stageGrouping.checkTerminationConditions());

        Assert.assertTrue(dummyStage4.isInitialized);
        Assert.assertEquals(dummyStage4, stageGrouping.getActiveStage());
        dummyStage4.terminate = true;
        Assert.assertTrue(dummyStage4.checkTerminationConditions());
        Assert.assertTrue(stageGrouping.checkTerminationConditions());

    }

}