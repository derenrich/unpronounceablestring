package util.heuristic;

import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

public class DepthCharge extends Heuristic {
	final int DEPTH_CHARGE_COUNT = 25;
	public DepthCharge(StateMachine sm, Role ourPlayer) {
		super(sm, ourPlayer);
	}

	@Override
	public float getScore(MachineState s) throws MoveDefinitionException,
			TransitionDefinitionException, GoalDefinitionException {
		int wins = 0;
		for(int i=0; i < DEPTH_CHARGE_COUNT ; i++){
			wins += charge(s);
		}
		return wins  / (float) DEPTH_CHARGE_COUNT;
	}
	
	private int charge(MachineState s) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		MachineState final_state = sm.performDepthCharge(s, null);
		if(sm.getGoal(final_state, this.ourPlayer) > 0){
			return 1;
		} else {
			return 0;
		}
	}
	
	@Override
	public String getName() {
		return "DepthCharge";
	}

}
