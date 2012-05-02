package util.heuristic;

import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.StateMachine;

public class DummyHeuristic extends Heuristic{

	DummyHeuristic(StateMachine sm, Role ourPlayer) {
		super(sm, ourPlayer);
	}

	@Override
	public float getScore(MachineState s) {
		return .5f;
	}

	@Override
	public String getName() {
		return "Dummy Heuristic";
	}

}
