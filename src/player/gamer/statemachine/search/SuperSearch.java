package player.gamer.statemachine.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import player.gamer.statemachine.StateMachineGamer;
import util.heuristic.CombinationHeuristic;
import util.heuristic.DepthCharge;
import util.heuristic.DummyHeuristic;
import util.heuristic.Heuristic;
import util.heuristic.MobilityHeuristic;
import util.heuristic.FocusHeuristic;
import util.heuristic.OpponentFocusHeuristic;
import util.heuristic.OpponentMobilityHeuristic;
import util.heuristic.Score;
import util.metagaming.EndBook;
import util.statemachine.CachedStateMachine;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import util.statemachine.implementation.prover.ProverStateMachine;

public class SuperSearch extends StateMachineGamer {
	
	CachedStateMachine cachedStateMachine;
	
	@Override
	public StateMachine getInitialStateMachine() {
		cachedStateMachine = new CachedStateMachine(new ProverStateMachine());
		return (StateMachine) cachedStateMachine;
	}
	Heuristic h;
	private int initial_search_depth = 2;
	MinimaxThread searcher;
	Thread search_thread;
	private ConcurrentHashMap<MachineState,Score> values;
	private ConcurrentHashMap<MachineState,Move> moves;
	final int HashCapacity = 10000000;
	final int MaxHashSize = 15000000;
	
	private Set<MachineState> wins;
	private Set<MachineState> losses;
	
	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		values = new ConcurrentHashMap<MachineState,Score>(HashCapacity);
		moves = new ConcurrentHashMap<MachineState,Move>(HashCapacity);
		//h = new DepthCharge(cachedStateMachine, this.getRole());
		h = new DummyHeuristic(cachedStateMachine, this.getRole());		
		EndBook book = new EndBook();
		wins = Collections.newSetFromMap(new ConcurrentHashMap<MachineState,Boolean>(20000));
		losses = Collections.newSetFromMap(new ConcurrentHashMap<MachineState,Boolean>(20000));				
		try {
			System.out.println("Generating endgame book");			
			book.generateBook(this.getCurrentState(), cachedStateMachine, this.getRole(), timeout, wins, losses);
			book.expandBook(this.getCurrentState(), cachedStateMachine, this.getRole(), wins, losses);
			System.out.println("Generated an endgame book of size: " + (wins.size() + losses.size()));			
		    searcher = new MinimaxThread(initial_search_depth);
		    search_thread = new Thread(searcher);
		    search_thread.start();
			//Thread.sleep(timeout - System.currentTimeMillis());
		    searcher.stop();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			e.printStackTrace();
		}		
	}		
	public void findMinMax(int depth) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		Score alpha = new Score();
		alpha.heuristicScore = 0;
		alpha.stateScore = 0;
		alpha.depth = 0;
		Score beta = new Score();
		beta.heuristicScore = 2;
		beta.stateScore = 200;
		beta.depth = 0;		
		iterMinMax(depth, 0, this.getCurrentState(), alpha , beta);		
	}
	
	// we prefer incomplete to getting nothing but prefer getting something to it
	private void iterMinMax(int max_depth, int depth, MachineState s, Score alpha, Score beta) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		StateMachine sm = this.getStateMachine();
		// have we not done this before?
		// have we done it before but worse? 
		if(!values.containsKey(s) || values.get(s).depth < game_depth + max_depth){
			// are we at the base-case?
			if(sm.isTerminal(s)){
				Score score = new Score();
				score.stateScore = getGoal(s);	
				score.depth = game_depth + depth;
				values.put(s, score);
			} else if (wins.contains(s)) {
				Score score = new Score();
				score.stateScore = 100; // this is a hack, should be the actual value
				score.depth = game_depth + depth;
				values.put(s, score);		
			} else if (losses.contains(s)) {
				Score score = new Score();
				score.stateScore = 0;		
				score.depth = game_depth + depth;
				values.put(s, score);		
			} else if(depth >= max_depth){
				Score score = new Score();
				score.heuristicScore = h.getScore(s);		
				score.depth = game_depth + depth;
				values.put(s, score);
			} else {
				List<Move> our_moves = sm.getLegalMoves(s, this.getRole());
				List<Role> opposing_roles = new ArrayList<Role>(sm.getRoles());
				opposing_roles.remove(this.getRole());				
				Move best_move = null;
				for(Move our_move : our_moves) {
					List<List<Move>> joint_moves = sm.getLegalJointMoves(s, this.getRole(), our_move);
					Score a = alpha;
					Score b = beta;
					// BEGIN VIRTUAL RECURSIVE CALL
					for(List<Move> joint_move : joint_moves) {
						MachineState next_state = sm.getNextState(s, joint_move);
						if(next_state == null){
							System.out.println("Error: Next state is null");
							// error in the GDL
							continue;
						}
						iterMinMax(max_depth, depth + 1, next_state, a, b);
						// did we run out of time?
						if(Thread.currentThread().isInterrupted()){
							return;
						}
						Score state_val = values.get(next_state);
						if (state_val.compareTo(b) < 0){ // state_val < b
							b = state_val;
						}
						if (b.compareTo(a) <= 0){ // b <= a
							break;
						}
					}
					// END VIRTUAL RECURISVE CALL
					if (b.compareTo(alpha) == 1 ) { // b > alpha
						best_move = our_move;
						alpha = b;
					}
					if (beta.compareTo(alpha) <= 0){ // beta <= alpha
						break;
					}
				}
				values.put(s, alpha);
				if(best_move != null){
					moves.put(s, best_move);
				} else {
					moves.put(s, our_moves.get(0));
				}
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
	private int game_depth = 0;
	private boolean first_move = true;
	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		System.out.println("Book size: "+ (wins.size()+losses.size()));
	    searcher.stop();
		search_thread.interrupt();
		if(!first_move){
			game_depth += 1;
		} 
		first_move = false;
		
		// check if we should blow away our caches
		if(values.size() > this.MaxHashSize) {			
			values = new ConcurrentHashMap<MachineState,Score>(HashCapacity);
			moves = new ConcurrentHashMap<MachineState,Move>(HashCapacity);
		}
	    try {
		    searcher = new MinimaxThread(initial_search_depth);
		    search_thread = new Thread(searcher);
		    search_thread.start();
			Thread.sleep(timeout - System.currentTimeMillis());
			System.out.println("Overslept by: " + (timeout - System.currentTimeMillis()));
		    searcher.stop();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	    
		Move final_move = moves.get(this.getCurrentState());
		
		if(final_move == null) {
			System.err.println(this.getClass() + ": Failed to get valid move in time. Random play.");			
			return this.getStateMachine().getRandomMove(this.getCurrentState(), this.getRole());
		} else {
			Score final_value = values.get(this.getCurrentState());
			System.out.println("Final value: " + final_value);
			System.out.println("Move: " + moves.get(this.getCurrentState()));
			return final_move;
		}
	}
	
	
    class MinimaxThread implements Runnable {
    	private int depth;
		private volatile boolean stop = false;
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
		return "SuperSearch";
	}
}
