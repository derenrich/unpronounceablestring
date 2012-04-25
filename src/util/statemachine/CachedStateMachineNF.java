package util.statemachine;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


import util.gdl.grammar.Gdl;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

public class CachedStateMachineNF extends StateMachine {
	final int CacheSize=100000;
	StateMachine sm;
	ConcurrentHashMap<MachineState,Boolean> terminalCache;
	ConcurrentHashMap<StateMovesPair,MachineState> nextCache;
	ConcurrentHashMap<StateRolePair,List<Move>> legalCache;
	public int getNumberStates(){
		return nextCache.size();
	}
	public CachedStateMachineNF(StateMachine sm){
		this.sm = sm;
		
		terminalCache = new ConcurrentHashMap<MachineState, Boolean>(CacheSize);
											   
		nextCache = new ConcurrentHashMap<StateMovesPair, MachineState>(CacheSize);
		legalCache = new ConcurrentHashMap<StateRolePair,List<Move>>(CacheSize);
		
	}
	
	public int getSize(){
		return terminalCache.size();
	}
	
	@Override
	public void initialize(List<Gdl> description) {
		sm.initialize(description);
	}
	private class StateRolePair{
		public MachineState state;
		public Role role;
		public StateRolePair(MachineState state, Role role){
			this.state = state;
			this.role = role;
		}
		public int hashCode(){
			return state.hashCode() + role.hashCode();
		}
		public boolean equals(Object obj){
		    if (!(obj instanceof StateRolePair))
		        return false;
		    StateRolePair srp = (StateRolePair) obj;
			return state.equals(srp.state) && role.equals(srp.role) ;
		}
	}

	@Override
	public int getGoal(MachineState state, Role role)
			throws GoalDefinitionException {
		return sm.getGoal(state, role);
	}
	@Override
	public boolean isTerminal(MachineState state) {
		Boolean cached_terminal = terminalCache.get(state);		
		if(cached_terminal == null){
			boolean terminal = sm.isTerminal(state);
			terminalCache.put(state, terminal);
			return terminal;
		}
		return cached_terminal.booleanValue();
	}

	@Override
	public List<Role> getRoles() {
		return sm.getRoles();
	}

	@Override
	public MachineState getInitialState() {
		return sm.getInitialState();
	}

	@Override
	public List<Move> getLegalMoves(MachineState state, Role role)
			throws MoveDefinitionException {
		StateRolePair srp = new StateRolePair(state,role);
		List<Move> moves = legalCache.get(srp);
		if(moves == null){
			moves = sm.getLegalMoves(state, role);
			legalCache.put(srp, moves);
		}
		return moves;
	}
	
	private class StateMovesPair{
		public MachineState state;
		public List<Move> moves;
		public StateMovesPair(MachineState state, List<Move> moves){
			this.state = state;
			this.moves = moves;
		}
		public int hashCode(){
			return state.hashCode() + moves.hashCode();
		}
		public boolean equals(Object obj){
		    if (!(obj instanceof StateMovesPair))
		        return false;
		    StateMovesPair smp = (StateMovesPair) obj;
			return state.equals(smp.state) && moves.equals(smp.moves) ;
		}
	}
	@Override
	public MachineState getNextState(MachineState state, List<Move> moves)
			throws TransitionDefinitionException {
		StateMovesPair pair =  new StateMovesPair(state,moves);
		MachineState nextState = nextCache.get(pair);
		if(nextState == null) {			
			nextState = sm.getNextState(state, moves);
			nextCache.put(pair, nextState);
		}
		return nextState;
	}

}
