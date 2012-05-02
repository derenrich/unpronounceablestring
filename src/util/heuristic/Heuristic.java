package util.heuristic;

import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

public abstract class Heuristic {
	protected StateMachine sm;
	protected Role ourPlayer;
	Heuristic(StateMachine sm, Role ourPlayer) {
		this.sm = sm;
		this.ourPlayer = ourPlayer;
	}
	/* must be between 0 and 1 */
	public abstract float getScore(MachineState s) throws MoveDefinitionException, TransitionDefinitionException;


}
