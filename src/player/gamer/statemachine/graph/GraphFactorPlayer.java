package player.gamer.statemachine.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

import player.gamer.statemachine.StateMachineGamer;
import util.heuristic.Score;
import util.search.AlphaBetaSearch;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import util.statemachine.implementation.propnet.PropNetStateMachine;
import util.statemachine.implementation.prover.ProverStateMachine;

public class GraphFactorPlayer extends StateMachineGamer {
	private ConcurrentHashMap<MachineState,Score> values;
	private ConcurrentHashMap<MachineState,Move> moves;
	final int HashCapacity = 10000000;
	final int MaxHashSize = 15000000;
	private int initial_search_depth = 2;
	int game_depth = 0;
	
	List<StateMachine> statemachines;
	List<AlphaBetaSearch> searchers;
	
	@Override
	public StateMachine getInitialStateMachine() {
		//return new ProverStateMachine();
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
		}
		statemachines = new ArrayList<StateMachine>();
		statemachines.add(this.getStateMachine());
		searchers = new ArrayList<AlphaBetaSearch>();
		for(StateMachine sm  : statemachines) {
			searchers.add(new AlphaBetaSearch (sm, this.getRole(), values, moves));
		}
		System.out.println("Overslept by: " + (timeout - System.currentTimeMillis()));
	}
	
	class ABRunner implements Runnable {
		public int search_depth;
		private MachineState ms;
		private int game_depth;
		private AlphaBetaSearch abs;
		public ABRunner(AlphaBetaSearch abs, int search_depth, MachineState ms, int game_depth) {
			this.search_depth = search_depth;
			this.ms = ms;
			this.abs = abs;
			this.game_depth = game_depth;
		}
		@Override
		public void run() {
        	try {
        		while(!Thread.interrupted()) {
        			MachineState reduced_state = abs.findMinMax(search_depth, ms, game_depth);
        			if(values.get(reduced_state).stateScore == 100) {
        				System.out.println("Found: " + System.currentTimeMillis());
        			}
        			search_depth += 1;        			
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
		for(AlphaBetaSearch abs : searchers) {
			Thread t = new Thread(new ABRunner(abs, search_depth, this.getCurrentState(), game_depth));
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
		return "Factorer";
	}

}
