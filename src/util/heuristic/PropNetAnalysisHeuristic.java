package util.heuristic;

import util.propnet.PropNetAnalysis;
import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

public class PropNetAnalysisHeuristic extends Heuristic {
	public PropNetAnalysisHeuristic(StateMachine sm, Role ourPlayer) {
		super(sm, ourPlayer);
	}
	private PropNetAnalysis pna;
	public void setPna(PropNetAnalysis pna) {
		this.pna = pna;
	}

	public PropNetAnalysisHeuristic(StateMachine sm, Role ourPlayer, PropNetAnalysis pna) {
		super(sm, ourPlayer);
		this.pna = pna;
	}
	
	@Override
	public float getScore(MachineState s) throws GoalDefinitionException,
			MoveDefinitionException, TransitionDefinitionException {
		return this.pna.maxValue(s, this.ourPlayer);
	}

	@Override
	public String getName() {
		return "PropNetAnalysisHeuristic";
	}

}
