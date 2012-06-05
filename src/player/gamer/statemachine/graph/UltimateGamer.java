package player.gamer.statemachine.graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import player.gamer.statemachine.StateMachineGamer;
import player.gamer.statemachine.graph.GraphFactorPlayer.ABRunner;
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
	    while(timeout - 50 > System.currentTimeMillis()) {
	    	Thread.yield();
	    }
		current_best_move = this.moves.get(this.getCurrentState());	
		System.out.println("Overslept by: " + -(timeout - System.currentTimeMillis()));
		game_depth +=1 ;
		System.out.println(current_best_move);
		current_best_move = null;
		if(current_best_move != null && legalMoves.contains(current_best_move)) {
			return current_best_move;
		} else {
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
