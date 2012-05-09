package util.heuristic;

import java.util.ArrayList;
import java.util.Random;

import util.statemachine.MachineState;

import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

public class CombinationHeuristic extends Heuristic{
	private Heuristic[] heuristics;
	private float[] weights;
	public CombinationHeuristic(StateMachine sm, Role ourPlayer, Heuristic[] hs, float[] ws) {
		super(sm, ourPlayer);
		assert(hs.length == ws.length);
		heuristics = hs;
		weights = ws;
	}
	public CombinationHeuristic(StateMachine sm, Role ourPlayer, Heuristic[] hs) {
		super(sm, ourPlayer);
		weights = new float[hs.length];
		heuristics = hs;
		autoSetWeights();
	}
	@Override
	public float getScore(MachineState s) throws MoveDefinitionException,
			TransitionDefinitionException, GoalDefinitionException {
		float score = 0;
		float total = 0;
		for( int i=0; i<heuristics.length;i++) {
			score += heuristics[i].getScore(s) * weights[i];
			total += weights[i];
		}
		return score / total;
	}
final float learning_rate = .1f;
final float decay_rate = .6f;
	public void autoSetWeights() {
		int won = 0;
		// Number of Depth Charges
		int numDC = 30;
		Random generator = new Random();
		for(int i=0; i<weights.length;i++) {
			weights[i] = generator.nextInt(50)+25;
		}
		ArrayList<MachineState> history;
		MachineState state;
		float goal;
		try {
			state = sm.getInitialState();
			for(int i=0;i<numDC;i++) {
				System.out.println("DC:"+i);
				history = new ArrayList<MachineState>();
				MachineState terminal = sm.performRememberingDepthCharge(state , history );
				goal = sm.getGoal(terminal, ourPlayer);
				if(goal>0)
					won++;
				
				// Apply learning.
				float eta = learning_rate;
				// Start looking at first choice before end.
				for (int j = history.size()-2; j >= 0; j--) {
					float[] scores = new float[weights.length];
					float sum = 0;
					// Precompute important values
					for(int w=0; w<weights.length; w++) {
						scores[w] = heuristics[w].getScore(history.get(j));
						sum += scores[w] * weights[w];
					}
					
					// Update weights via gradient descent
					for(int w=0; w<weights.length; w++) {
						weights[w] = weights[w] - eta*2*(sum-goal)*scores[w];
					}
					eta *= decay_rate;
				}
				
			}
		} catch (TransitionDefinitionException e) {
			System.err.println("Transition Error!!");
			e.printStackTrace();
		} catch (MoveDefinitionException e) {
			System.err.println("Move Error!!");
			e.printStackTrace();
		} catch (GoalDefinitionException e) {
			System.err.println("Depth Charge did not end in terminal!!");
			e.printStackTrace();
		}
		for(int i=0;i<weights.length;i++) {
			System.out.println("Weight for "+heuristics[i].getName()+" is "+weights[i]);
		}
		System.out.println("Percent won = "+(100*(float)won/(float)numDC));
	}
	
	@Override
	public String getName() {
		return "Combination Heuristic";
	}

}
