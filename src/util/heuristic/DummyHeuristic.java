package util.heuristic;

import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.StateMachine;

public class DummyHeuristic extends Heuristic{

	DummyHeuristic(StateMachine sm, Role ourPlayer) {
		super(sm, ourPlayer);
		// TODO Auto-generated constructor stub
	}

	@Override
	float getScore(MachineState s) {
		// TODO Auto-generated method stub
		return .5f;
	}

	@Override
	String getName() {
		// TODO Auto-generated method stub
		return "Dummy Heuristic";
	}

}
