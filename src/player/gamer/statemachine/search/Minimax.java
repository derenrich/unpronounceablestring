package player.gamer.statemachine.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import player.gamer.statemachine.StateMachineGamer;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import util.statemachine.implementation.prover.ProverStateMachine;

public class Minimax extends StateMachineGamer {

	@Override
	public StateMachine getInitialStateMachine() {
		return new ProverStateMachine();
	}
	private int search_depth=1;
	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		search_depth = estimateDepth(50,4);
	}
	
	private int estimateDepth(int trials, int max_depth) throws TransitionDefinitionException, MoveDefinitionException{
		int min_depth = max_depth;
		trials = trials > 0 ? trials : 0;
		for(int i = 0; i < trials; i++) {
			// depth charge destroys the current state so we first take a single step
			// which clones the state for us
			MachineState state = getStateMachine().getRandomNextState(this.getCurrentState());
			int[] depth = new int[1];
			getStateMachine().performDepthCharge(state, depth);
			// add one to account for our first step
			min_depth = Math.min(min_depth, depth[0] + 1);
		}
		return min_depth;
	}
	private HashMap<MachineState,Integer> values;
	private HashMap<MachineState,Move> moves;
	private HashMap<MachineState,Integer> depths;
	
	public void findMinMax(int depth) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		values = new HashMap<MachineState,Integer>();
		depths = new HashMap<MachineState,Integer>();
		moves = new HashMap<MachineState,Move>();
		iterMinMax(depth, this.getCurrentState());		
	}
	private void iterMinMax(int depth, MachineState s) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		StateMachine sm = this.getStateMachine();
		// is it worth recursing?
		if(!depths.containsKey(s) || depths.get(s) < depth){
			if(sm.isTerminal(s)){
				int score = getGoal(s);
				values.put(s, score);
				depths.put(s, depth);
			} else if(depth <= 0){
				values.put(s, 1);
				depths.put(s, depth);				
			}else {
				List<Move> our_moves = sm.getLegalMoves(s, this.getRole());
				List<Role> opposing_roles = new ArrayList<Role>(sm.getRoles());
				opposing_roles.remove(this.getRole());				
				Move best_move = null;
				int max_min_val = 0;
				
				for(Move our_move : our_moves) {
					int min_val = 100;
					for(List<Move> joint_move : sm.getLegalJointMoves(s, this.getRole(), our_move)) {
						MachineState next_state = sm.getNextState(s, joint_move);
						iterMinMax(depth - 1, next_state);
						min_val = Math.min(min_val, values.get(next_state));
					}
					if (min_val >= max_min_val){
						max_min_val = min_val;
						best_move = our_move;
					}
				}
				values.put(s, max_min_val);
				depths.put(s, depth);
				moves.put(s, best_move);				
			}
		}
	}
	
	private int getGoal(MachineState s) throws GoalDefinitionException{
		if(this.getStateMachine().isTerminal(s)){
			return this.getStateMachine().getGoal(s, this.getRole());
		} else {
			return 0;
		}
	}
	
	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {		
		findMinMax(4);
		Move final_move = moves.get(this.getCurrentState());
		return final_move;
	}

	@Override
	public void stateMachineStop() {
		// TODO Auto-generated method stub

	}

	@Override
	public void stateMachineAbort() {
		// TODO Auto-generated method stub

	}
	@Override
	public String getName() {
		return "Minimax";
	}
}
