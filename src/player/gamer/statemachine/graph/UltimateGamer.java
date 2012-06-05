package player.gamer.statemachine.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import player.gamer.statemachine.StateMachineGamer;
import player.gamer.statemachine.graph.GraphFactorPlayer.ABRunner;
import util.depthcharger.DCScore;
import util.depthcharger.NaiveDepthCharger;
import util.heuristic.Heuristic;
import util.heuristic.PropNetAnalysisHeuristic;
import util.heuristic.Score;
import util.propnet.PropNetAnalysis;
import util.search.AlphaBetaSearch;
import util.statemachine.CachedStateMachine;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import util.statemachine.implementation.propnet.PropNetStateMachine;
import util.statemachine.implementation.propnet.SpeedyPropNetStateMachine;

public class UltimateGamer extends StateMachineGamer {
	private ConcurrentHashMap<MachineState,Score> values;
	private ConcurrentHashMap<MachineState,Move> moves;
	final int HashCapacity = 10000000;
	final int MaxHashSize = 15000000;
	final int DEPTH_THREAD_NUM = 2;
	final int SAFETY_MARGIN = 500;
	private int initial_search_depth = 2;
	int game_depth = 0;
	Thread main_thread;
	
	AlphaBetaSearch searcher;

	
	@Override
	public StateMachine getInitialStateMachine() {
		PropNetStateMachine pnsm = new PropNetStateMachine();
		pnsm.initialize(this.match.getGame().getRules());
		SpeedyPropNetStateMachine spnsm = new SpeedyPropNetStateMachine(pnsm);
		CachedStateMachine cspnsm = new CachedStateMachine(spnsm);
		return cspnsm;
	}

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		values = new ConcurrentHashMap<MachineState,Score>(HashCapacity);
		moves = new ConcurrentHashMap<MachineState,Move>(HashCapacity);		
		searcher = new AlphaBetaSearch(this.getInitialStateMachine(), this.getRole(), values, moves);
		
		// KICK OFF MORE THREADS?
		
		System.out.println("Meta Game Overslept by: " + (timeout - System.currentTimeMillis()));
	}

	class DCRunner implements Runnable {
		private NaiveDepthCharger charger;
		public DCRunner(StateMachine sm, Role ourPlayer,
				List<MachineState> startStates,
				ConcurrentHashMap<MachineState, DCScore> c) {
			charger = new NaiveDepthCharger(sm, ourPlayer, startStates,	c);
		}
		@Override
		public void run() {
			charger.run_charges();
		}
	}	
	
	
	class ABRunner implements Runnable {
		public int search_depth;
		private MachineState ms;
		private int game_depth;
		private AlphaBetaSearch abs;
		private Role role;
		public ABRunner(AlphaBetaSearch abs, int search_depth, MachineState ms, int game_depth, Role role) {
			this.search_depth = search_depth;
			this.ms = ms;
			this.abs = abs;
			this.game_depth = game_depth;
			this.role = role;
		}
		@Override
		public void run() {
        	try {
        		while(!Thread.interrupted()) {
        			abs.findMinMax(search_depth, ms, game_depth);
        			search_depth += 1; 
        			if(search_depth <= 15) {
        				System.out.println("Looked ahead " + search_depth);
        			}
        		}
			} catch (GoalDefinitionException e) {
				e.printStackTrace();
			} catch (MoveDefinitionException e) {
				e.printStackTrace();
			} catch (TransitionDefinitionException e) {
				e.printStackTrace();
			}
		}
	}	
	
	public List<Move> bestMoves(List<List<Move>> legalJointMoves) throws TransitionDefinitionException {		
		Map<Integer, Integer> foo = new HashMap<Integer, Integer>();		
		Score bestScore = values.get(getCurrentState());
		List<Move> bestMoveList = new ArrayList();
		// Set<Move> ourMoves = new HashSet(this.getStateMachine().getLegalMoves(getCurrentState(), this.getRole()));
		for(List<Move> jointMove : legalJointMoves) {
			MachineState nextState = this.getStateMachine().getNextState(getCurrentState(), jointMove);
			if(!values.containsKey(nextState)) {
				// was pruned
				continue;
			}
			if(values.get(nextState).stateScore == bestScore.stateScore) {
				int roleIdx = this.getStateMachine().getRoleIndices().get(this.getRole());
				bestMoveList.add(jointMove.get(roleIdx));
			}
		}
		return bestMoveList;
	}
	
	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		long time = System.currentTimeMillis();
	    try {
	    	if(main_thread != null) {
	    		main_thread.interrupt();    	
	    		main_thread.join();
	    	}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	    System.out.println("Join time : " + (System.currentTimeMillis() - time));
		Move current_best_move = null;
		int search_depth = initial_search_depth;
		System.out.println("State: " + this.getCurrentState());
		System.out.println("Start: " + System.currentTimeMillis());
		main_thread = new Thread(new ABRunner(searcher, search_depth, this.getCurrentState(), game_depth, this.getRole()));
		main_thread.start();		
		List<Move> legalMoves = this.getStateMachine().getLegalMoves(getCurrentState(), getRole());
		ArrayList<Thread> chargeThreads = new ArrayList<Thread>();
		List<List<Move>> legalJointMoves = null;
		ConcurrentHashMap<MachineState, DCScore> state_values = new ConcurrentHashMap<MachineState, DCScore>();
		boolean depthCharging = false;
		if(legalMoves.size() > 1 ) {
			depthCharging = true;
			/* DEPTH CHARGE CODE */
			legalJointMoves = this.getStateMachine().getLegalJointMoves(getCurrentState());
			List<MachineState> states = new ArrayList<MachineState>();
			for(List<Move> moves : legalJointMoves) {
				states.add(this.getStateMachine().getNextState(getCurrentState(), moves));
			}
			List<List<MachineState>> initChargeStates = new ArrayList<List<MachineState>>();
			for(int i=0; i < DEPTH_THREAD_NUM; i++) {
				initChargeStates.add(new ArrayList<MachineState>());
			}
			for(int i=0; i < states.size(); i++) {
				initChargeStates.get(i % DEPTH_THREAD_NUM).add(states.get(i));
			}
			for(int i=0; i < DEPTH_THREAD_NUM; i++) {
				chargeThreads.add(new Thread(new DCRunner(this.getStateMachine(), 
															this.getRole(),
															initChargeStates.get(i),
															state_values)));
			}
			for(Thread t: chargeThreads) {
				t.start();
			}
		}
	    while(timeout - SAFETY_MARGIN > System.currentTimeMillis());
	    for(Thread t : chargeThreads) {
	    	t.interrupt();
	    }
		current_best_move = this.moves.get(this.getCurrentState());	
		System.out.println("Overslept by: " + -(timeout - System.currentTimeMillis()));
		game_depth +=1 ;
		if(depthCharging && values.get(getCurrentState()).stateScore <= 0) {
			List<Move> candidateMoves = bestMoves(legalJointMoves);
			Map<Move, Double> moveValues = new HashMap<Move, Double>();
			for(Move m : this.getStateMachine().getLegalMoves(getCurrentState(), getRole())) {
				if(candidateMoves.contains(m))
					moveValues.put(m, 100.0);
			}
			for(List<Move> m : legalJointMoves) {
				Move our_move = m.get(this.getStateMachine().getRoleIndices().get(this.getRole()));
				if(!candidateMoves.contains(our_move)) {
					continue;
				}
				double prevValue = moveValues.get(our_move);
				double newValue = Math.min(prevValue, (100* state_values.get(this.getStateMachine().getNextState(getCurrentState(), m)).value()));
				moveValues.put(our_move, newValue);
			}
			double current_best_score = 0;
			for(Move m : moveValues.keySet()) {
				if(moveValues.get(m) >= current_best_score) {
					current_best_score = moveValues.get(m); 
					current_best_move = m;
				}
			}
			System.out.println("Playing monte carlo move w/ score: " + current_best_score);
			System.out.println("Candidates: " + moveValues);			
		}
		System.out.println("Board evaluation: " + values.get(getCurrentState()));
		if (current_best_move != null && legalMoves.contains(current_best_move)) {
			System.out.println("Making move: " + current_best_move);
			return current_best_move;
		} else {
			System.out.println("Error occured. Playing randomly");
			Random r = new Random();
			return legalMoves.get((int) (r.nextDouble() * legalMoves.size()));
		}
	}
	@Override
	public void stateMachineStop() {
		
	}
	
	@Override
	public void stateMachineAbort() {

	}
	@Override
	public String getName() {
		return "UnPro";
	}
}
