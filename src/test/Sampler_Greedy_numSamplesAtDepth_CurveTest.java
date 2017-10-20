package test;

import static org.junit.Assert.*;
import org.junit.Test;

import main.Evaluator_Random;
import main.IEvaluationFunction;
import main.Sampler_Greedy;

public class Sampler_Greedy_numSamplesAtDepth_CurveTest {

	/**
	 * Test method for {@link Sampler_Greedy#numSamplesAtDepth(int depth)}.
	 */
	@Test
	public final void test_numSamplesAtDepth() {
		IEvaluationFunction evaluatorRandom = new Evaluator_Random();
		Sampler_Greedy sampler = new Sampler_Greedy(evaluatorRandom);
		assertEquals("Is line correct at depth 0? ", (int)sampler.samplesAt0,sampler.numSamplesAtDepth(0));
		assertEquals("Is line correct at depth N?", (int)sampler.samplesAtN,sampler.numSamplesAtDepth((int)sampler.depthN));
		assertEquals("Is line correct at depth (almost) infinity?", (int)sampler.samplesAtInf,sampler.numSamplesAtDepth(1000000));
	}

}
