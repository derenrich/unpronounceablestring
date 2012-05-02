package util.heuristic;

import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.StateMachine;

public class DummyHeuristic extends Heuristic{

<<<<<<< HEAD
	public DummyHeuristic(StateMachine sm) {
		super(sm);
=======
	DummyHeuristic(StateMachine sm, Role ourPlayer) {
		super(sm, ourPlayer);
		// TODO Auto-generated constructor stub
>>>>>>> 40c5aa222960e24671e766be3a3c3f7f31484e9e
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
