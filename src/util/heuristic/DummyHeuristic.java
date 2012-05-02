package util.heuristic;

import util.statemachine.MachineState;
import util.statemachine.StateMachine;

public class DummyHeuristic extends Heuristic{

	public DummyHeuristic(StateMachine sm) {
		super(sm);
	}

	@Override
	public float getScore(MachineState s) {
		// TODO Auto-generated method stub
		return .5f;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "Dummy Heuristic";
	}

}
