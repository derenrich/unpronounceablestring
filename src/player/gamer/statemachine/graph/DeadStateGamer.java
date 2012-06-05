package player.gamer.statemachine.graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import player.gamer.statemachine.StateMachineGamer;
import player.gamer.statemachine.graph.GraphFactorPlayer.ABRunner;
import util.heuristic.Heuristic;
import util.heuristic.PropNetAnalysisHeuristic;
import util.heuristic.Score;
import util.propnet.PropNetAnalysis;
import util.search.AlphaBetaSearch;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import util.statemachine.implementation.propnet.PropNetStateMachine;

public class DeadStateGamer extends StateMachineGamer {
	private ConcurrentHashMap<MachineState,Score> values;
	private ConcurrentHashMap<MachineState,Move> moves;
	final int HashCapacity = 10000000;
	final int MaxHashSize = 15000000;
	private int initial_search_depth = 2;
	int game_depth = 0;
	
	List<StateMachine> statemachines;
	List<AlphaBetaSearch> searchers;
	List<PropNetAnalysis> pnas;

	
	@Override
	public StateMachine getInitialStateMachine() {
		return new PropNetStateMachine();
	}

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		values = new ConcurrentHashMap<MachineState,Score>(HashCapacity);
		moves = new ConcurrentHashMap<MachineState,Move>(HashCapacity);
		
		// initialize the PropNet
		if(this.getStateMachine() instanceof PropNetStateMachine) {
			((PropNetStateMachine) this.getStateMachine()).initialize(this.match.getGame().getRules());
			//statemachines = ((PropNetStateMachine) this.getStateMachine()).splitGames();
			statemachines = new ArrayList<StateMachine>();
			statemachines.add(this.getStateMachine());
		} else {
			statemachines = new ArrayList<StateMachine>();
			statemachines.add(this.getStateMachine());
		}		
		searchers = new ArrayList<AlphaBetaSearch>();
		pnas = new ArrayList<PropNetAnalysis>();

		for(StateMachine sm  : statemachines) {
			searchers.add(new AlphaBetaSearch (sm, this.getRole(), values, moves));
			if(sm instanceof PropNetStateMachine) {
				PropNetAnalysis pna = new PropNetAnalysis(((PropNetStateMachine)sm).propNet);
				pnas.add(pna);
			}
		}
		System.out.println("Using " + searchers.size() + " state machines");
		System.out.println("Overslept by: " + (timeout - System.currentTimeMillis()));
	}
	
	class ABRunner implements Runnable {
		public int search_depth;
		private MachineState ms;
		private int game_depth;
		private AlphaBetaSearch abs;
		private PropNetAnalysis analysis;
		private Role role;
		public ABRunner(AlphaBetaSearch abs, int search_depth, MachineState ms, int game_depth, PropNetAnalysis analysis, Role role) {
			this.search_depth = search_depth;
			this.ms = ms;
			this.abs = abs;
			this.game_depth = game_depth;
			this.analysis = analysis;
			this.role = role;
			Heuristic pna = new PropNetAnalysisHeuristic(null, role, analysis);
			Set<Heuristic> oracles = new HashSet<Heuristic>();
			oracles.add(pna);
			abs.setOracles(oracles);
		}
		@Override
		public void run() {
        	try {
        		while(!Thread.interrupted()) {
        			MachineState reduced_state = abs.findMinMax(search_depth, ms, game_depth);
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
		Move current_best_move = null;
		int search_depth = initial_search_depth;
		System.out.println("State: " + this.getCurrentState());
		System.out.println("Start: " + System.currentTimeMillis());
		List<Thread> threads = new ArrayList<Thread>();
		for(int smidx = 0; smidx < searchers.size(); smidx++) {
			Thread t = new Thread(new ABRunner(searchers.get(smidx), search_depth, this.getCurrentState(), game_depth, pnas.get(smidx), this.getRole()));
			t.start();
			threads.add(t);
		}
	    while(timeout - 50 > System.currentTimeMillis()) {
	    	Thread.yield();
	    }
	    for(Thread t : threads) {	    	
	    	t.interrupt();
	    }
		current_best_move = this.moves.get(this.getCurrentState());	
		System.out.println("Overslept by: " + -(timeout - System.currentTimeMillis()));
		game_depth +=1 ;
		System.out.println(current_best_move);
		return current_best_move;
	}
	@Override
	public void stateMachineStop() {
		
	}
	
	@Override
	public void stateMachineAbort() {

	}
	@Override
	public String getName() {
		return "DeadStates";
	}
}
