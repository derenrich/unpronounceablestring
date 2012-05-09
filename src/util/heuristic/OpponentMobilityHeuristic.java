package util.heuristic;

import java.util.ArrayList;
import java.util.List;

import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

public class OpponentMobilityHeuristic extends Heuristic{
	private int depth;
	public OpponentMobilityHeuristic(StateMachine sm, Role currentPlayer) {
		this(sm,currentPlayer,0);
	}
	public OpponentMobilityHeuristic(StateMachine sm, Role currentPlayer, int depth) {
		super(sm, currentPlayer);
		this.depth = depth;
	}

	@Override
	public 	float getScore(MachineState s) throws MoveDefinitionException, TransitionDefinitionException {
		return 1-1/getScore(s, 0);
	}
	
	float getScore(MachineState s, int d) throws MoveDefinitionException, TransitionDefinitionException {
		if(d >= this.depth) {
			List<Role> opposing_roles = new ArrayList<Role>(sm.getRoles());
			opposing_roles.remove(this.ourPlayer);
			List<List<Move>> their_moves = sm.getLegalJointMoves(s, opposing_roles);
			return their_moves.size();
		} else {
			float sum = 0f;
				for(List<Move> joint_move : sm.getLegalJointMoves(s)) {
					MachineState next_state = sm.getNextState(s, joint_move);
					if(sm.isTerminal(next_state))
						return 1;
					else
						sum += getScore(next_state, d+1);
				}
			return sum;
		}
	}

	@Override
	public String getName() {
		return "Opponent Mobility Heuristic";
	}

}
