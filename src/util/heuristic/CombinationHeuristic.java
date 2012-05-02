package util.heuristic;

import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

public class CombinationHeuristic extends Heuristic{
private Heuristic[] heuristics;
private float[] weights;
	CombinationHeuristic(StateMachine sm, Role ourPlayer, Heuristic[] hs, float[] ws) {
		super(sm, ourPlayer);
		assert(hs.length == ws.length);
		heuristics = hs;
		weights = ws;
	}
	@Override
	float getScore(MachineState s) throws MoveDefinitionException,
			TransitionDefinitionException {
		float score = 0;
		float total = 0;
		for( int i=0; i<heuristics.length;i++) {
			score += heuristics[i].getScore(s) * weights[i];
			total += weights[i];
		}
		return score / total;
	}

	@Override
	String getName() {
		return "Combination Heuristic";
	}

}
