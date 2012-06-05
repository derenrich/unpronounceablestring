package util.search;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import util.heuristic.DummyHeuristic;
import util.heuristic.Heuristic;
import util.heuristic.Score;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

public class AlphaBetaSearch {
	private StateMachine sm;
	private Role r;
	private ConcurrentHashMap<MachineState,Score> values;
	private ConcurrentHashMap<MachineState,Move> moves;
	private Set<MachineState> wins;
	public Set<MachineState> getWins() {
		return wins;
	}
	public void setWins(Set<MachineState> wins) {
		this.wins = wins;
	}
	public void setLosses(Set<MachineState> losses) {
		this.losses = losses;
	}

	private Set<MachineState> losses;
	
	private Set<Heuristic> oracles;
	public void setOracles(Set<Heuristic> oracles) {
		this.oracles = oracles;
	}

	private Heuristic h;
	public AlphaBetaSearch(StateMachine sm,
						   Role r,
						   ConcurrentHashMap<MachineState,Score> values,
						   ConcurrentHashMap<MachineState,Move> moves) {
		this.sm = sm;
		this.r = r;
		this.values = values;
		this.moves = moves;
		wins = new HashSet<MachineState>();
		losses = new HashSet<MachineState>();
		h = new DummyHeuristic(sm, r);
		oracles = new HashSet<Heuristic>();

	}
	private int getGoal(MachineState s) throws GoalDefinitionException{
		if(sm.isTerminal(s)){
			return sm.getGoal(s, r);
		} else {
			return 0;
		}
	}

	public MachineState findMinMax(int depth, MachineState state, int game_depth) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		state = sm.getReducedState(state);
		Score alpha = new Score();
		alpha.heuristicScore = 0;
		alpha.stateScore = 0;
		alpha.depth = 0;
		Score beta = new Score();
		beta.heuristicScore = 2;
		beta.stateScore = 200;
		beta.depth = 0;		
		iterMinMax(depth, 0, state, alpha , beta, game_depth);
		return state;
	}
	
	private void iterMinMax(int max_depth, int depth, MachineState s, Score alpha, Score beta, int game_depth) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		// did we run out of time?
		if(Thread.currentThread().isInterrupted()){
			return;
		}
		StateMachine sm = this.sm;
		// have we not done this before?
		// have we done it before but worse? 
		if(!values.containsKey(s) || values.get(s).depth < game_depth + max_depth){			
			// are we at the base-case?
			if(sm.isTerminal(s)){
				Score score = new Score();
				score.stateScore = getGoal(s);	
				score.depth = game_depth + depth;
				values.put(s, score);
			} else if(depth >= max_depth){
				Score score = new Score();
				score.heuristicScore = h.getScore(s);		
				score.depth = game_depth + depth;
				values.put(s, score);
			} else if(wins.contains(s)) {
				Score score = new Score();
				score.stateScore = 100;		
				score.depth = game_depth + depth;
				values.put(s, score);
			} else if(losses.contains(s)) {
				Score score = new Score();
				score.stateScore = 0;		
				score.depth = game_depth + depth;
				values.put(s, score);
			} else {
				List<Move> our_moves = sm.getLegalMoves(s, r);
				List<Role> opposing_roles = new ArrayList<Role>(sm.getRoles());
				opposing_roles.remove(r);				
				Move best_move = null;
				for(Move our_move : our_moves) {
					List<List<Move>> joint_moves = sm.getLegalJointMoves(s, r, our_move);
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
						iterMinMax(max_depth, depth + 1, next_state, a, b, game_depth);
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
}
