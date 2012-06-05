package util.depthcharger;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.StateMachine;

public abstract class DepthCharger {
	protected StateMachine sm;
	protected Role ourPlayer;
	protected ConcurrentHashMap<MachineState, DCScore> scores;
	protected List <MachineState> startStates;
	public DepthCharger(StateMachine sm, Role ourPlayer, List <MachineState> startStates, ConcurrentHashMap<MachineState, DCScore> c) {
		this.sm = sm;
		this.ourPlayer = ourPlayer;
		this.startStates = startStates;
		this.scores = c;
		for (MachineState s : startStates) {
			c.put(s, new DCScore());
		}
	}
	
	public abstract void run_charges();
}
