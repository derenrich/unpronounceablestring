package util.heuristic;

import util.statemachine.MachineState;
import util.statemachine.StateMachine;

public abstract class Heuristic {
	private StateMachine sm;
	Heuristic(StateMachine sm) {
		this.sm = sm;
	}
	/* must be between 0 and 1 */
	abstract float getScore(MachineState s);
	abstract String getName();
}
