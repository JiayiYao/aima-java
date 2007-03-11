package aima.test.probreasoningtest;

import junit.framework.TestCase;
import aima.probability.RandomVariable;
import aima.probability.reasoning.HMMFactory;
import aima.probability.reasoning.HiddenMarkovModel;
import aima.probability.reasoning.HmmConstants;

public class HMMTest extends TestCase {
	private HiddenMarkovModel robotHmm,rainmanHmm;
	private static final double TOLERANCE = 0.001;
	public void setUp(){
		robotHmm = HMMFactory.createRobotHMM();
		rainmanHmm = HMMFactory.createRainmanHMM();
	}
	
	public void testRobotHMMInitialization(){
		
		assertEquals(0.5, robotHmm.prior().getProbabilityOf(HmmConstants.DOOR_OPEN));
		assertEquals(0.5, robotHmm.prior().getProbabilityOf(HmmConstants.DOOR_CLOSED));
	}
	
	public void testRainmanHmmInitialization(){
		
		assertEquals(0.5, rainmanHmm.prior().getProbabilityOf(HmmConstants.RAINING));
		assertEquals(0.5, rainmanHmm.prior().getProbabilityOf(HmmConstants.NOT_RAINING));
	}
	
	public void testForwardMessagingWorksForFiltering(){
		RandomVariable afterOneStep =  robotHmm.forward(robotHmm.prior(),HmmConstants.DO_NOTHING,HmmConstants.SEE_DOOR_OPEN);
		assertEquals(0.75, afterOneStep.getProbabilityOf(HmmConstants.DOOR_OPEN),TOLERANCE);
		assertEquals(0.25, afterOneStep.getProbabilityOf(HmmConstants.DOOR_CLOSED),TOLERANCE);
		
		RandomVariable afterTwoSteps = robotHmm.forward(afterOneStep,HmmConstants.PUSH_DOOR,HmmConstants.SEE_DOOR_OPEN);
		assertEquals(0.983, afterTwoSteps.getProbabilityOf(HmmConstants.DOOR_OPEN),TOLERANCE);
		assertEquals(0.017, afterTwoSteps.getProbabilityOf(HmmConstants.DOOR_CLOSED),TOLERANCE);
	}
	
	public void testRainmanHMMBackwarStepModifiesBeliefCorrectly(){
	    RandomVariable afterOneStep = rainmanHmm.forward(rainmanHmm.prior(),HmmConstants.DO_NOTHING,HmmConstants.SEE_UMBRELLA);
	    RandomVariable afterTwoSteps = rainmanHmm.forward(afterOneStep,HmmConstants.DO_NOTHING,HmmConstants.SEE_UMBRELLA);
	    
	    RandomVariable postSequence = afterTwoSteps.duplicate();
	    postSequence.setProbabilityOf(HmmConstants.RAINING,1.0);
	    postSequence.setProbabilityOf(HmmConstants.NOT_RAINING,1.0);
	    
	    RandomVariable smoothed = rainmanHmm.step_backward(afterOneStep,postSequence,HmmConstants.SEE_UMBRELLA);
	    assertEquals(0.883, smoothed.getProbabilityOf(HmmConstants.RAINING),TOLERANCE);
	    assertEquals(0.117, smoothed.getProbabilityOf(HmmConstants.NOT_RAINING),TOLERANCE);
	    
	    
	}
}
