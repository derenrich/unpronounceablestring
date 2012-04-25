package player.gamer.statemachine.search;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import player.gamer.statemachine.StateMachineGamer;
import util.statemachine.CachedStateMachine;
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
		return new CachedStateMachine(new ProverStateMachine());
		//return new ProverStateMachine();
	}
	private int initial_search_depth = 3;
	MinimaxThread searcher;
	Thread search_thread;
	
	final int HashCapacity = 300000;
	final int MaxHashSize = 500000;
	
	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		values = new ConcurrentHashMap<MachineState,Float>(HashCapacity);
		depths = new ConcurrentHashMap<MachineState,Integer>(HashCapacity);
		moves = new ConcurrentHashMap<MachineState,Move>(HashCapacity);
	    try {
		    searcher = new MinimaxThread(initial_search_depth);
		    search_thread = new Thread(searcher);
		    search_thread.start();
			Thread.sleep(timeout - System.currentTimeMillis());
		    searcher.stop();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
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
	private ConcurrentHashMap<MachineState,Float> values;
	private ConcurrentHashMap<MachineState,Move> moves;
	private ConcurrentHashMap<MachineState,Integer> depths;
	
	/*
	 * Encode extra information in the goal value
	 * The closer a good goal is monotonically increases the goal
	 * The closer a bad goal is monotonically decreases the goal
	 * Does not change the integer value of the goal (only fractional bit)
	 * Deep thinking was involved here
	 */
	private static float decorateGoal(float goal){
		if (goal < 0.5){
			return (float) (goal + (0.5 - goal)/4);
		} else if(goal > 0.5){
			return (float) (goal + (Math.floor(goal + 0.5) - 0.5f - goal)/4.0) ;
		} else {
			return goal;
		}
		
	}
	public void findMinMax(int depth) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		iterMinMax(depth, this.getCurrentState());		
	}
	// we prefer incomplete to getting nothing but prefer getting something to it
	final static float INCOMPLETE_SEARCH_VALUE = 0.5f;
	private void iterMinMax(int depth, MachineState s) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		StateMachine sm = this.getStateMachine();
		// have we done this before?
		if(!depths.containsKey(s) || depths.get(s) < depth){
			// are we at the base-case?
			if(sm.isTerminal(s)){
				int score = getGoal(s);
				values.put(s, (float) score);
				depths.put(s, depth);
			} else if(depth == 0){
				values.put(s, INCOMPLETE_SEARCH_VALUE);
				depths.put(s, depth);			
			} else {
				List<Move> our_moves = sm.getLegalMoves(s, this.getRole());
				List<Role> opposing_roles = new ArrayList<Role>(sm.getRoles());
				opposing_roles.remove(this.getRole());				
				Move best_move = null;
				float max_min_val = 0;
				for(Move our_move : our_moves) {
					float min_val = 100;
					List<List<Move>> joint_moves = sm.getLegalJointMoves(s, this.getRole(), our_move);
					for(List<Move> joint_move : joint_moves) {
						MachineState next_state = sm.getNextState(s, joint_move);
						if(next_state == null){
							// error in the GDL
							continue;
						}
						iterMinMax(depth - 1, next_state);
						// did we run out of time?
						if(Thread.currentThread().isInterrupted()){
							return;
						}
						float state_val = decorateGoal(values.get(next_state));
						min_val = Math.min(min_val, state_val);
						// hacky alpha beta
						if(min_val < 0.5){
							break;
						}
					}
					if(min_val >= max_min_val){
						max_min_val = min_val;
						best_move = our_move;
					}
					// hacky alpha beta
					if(max_min_val > 99){
						break;
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
	    searcher.stop();
		search_thread.interrupt();
		// check if we should blow away our caches
		if(values.size() > this.MaxHashSize) {			
			values = new ConcurrentHashMap<MachineState,Float>(HashCapacity);
			depths = new ConcurrentHashMap<MachineState,Integer>(HashCapacity);
			moves = new ConcurrentHashMap<MachineState,Move>(HashCapacity);
		}
	    try {
		    searcher = new MinimaxThread(initial_search_depth);
		    search_thread = new Thread(searcher);
		    search_thread.start();
			Thread.sleep(timeout - System.currentTimeMillis());
		    searcher.stop();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	    
		Move final_move = moves.get(this.getCurrentState());
		
		if(final_move == null) {
			System.err.println("Minimax: Failed to get valid move in time. Random play.");			
			return this.getStateMachine().getRandomMove(this.getCurrentState(), this.getRole());
		} else {
			float final_value = values.get(this.getCurrentState());
			System.out.println("Final value: " + final_value);
			return final_move;
		}
	}
	
	
    class MinimaxThread implements Runnable {
    	private int depth;
		private boolean stop = false;
    	MinimaxThread(int depth){
    		this.depth = depth;
    	}
		@Override
		public void run() {		
			stop = false;
			int cur_depth = depth;
			try {
				while(!stop){
					findMinMax(cur_depth);
					if(!Thread.currentThread().isInterrupted() && cur_depth < 100){
						System.out.println("looked: " + cur_depth);
					}
					cur_depth+=1;
				}
			} catch (GoalDefinitionException e) {
				e.printStackTrace();
			} catch (MoveDefinitionException e) {
				e.printStackTrace();
			} catch (TransitionDefinitionException e) {
				e.printStackTrace();
			}
		}
		public void stop(){
			stop = true;
		}
    }
	@Override
	public void stateMachineStop() {
	    searcher.stop();
		search_thread.interrupt();
	}

	@Override
	public void stateMachineAbort() {
	    searcher.stop();
		search_thread.interrupt();
	}
	@Override
	public String getName() {
		return "Minimax";
	}
}
