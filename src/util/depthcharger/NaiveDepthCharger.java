package util.depthcharger;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

public class NaiveDepthCharger extends DepthCharger{

	public NaiveDepthCharger(StateMachine sm, Role ourPlayer,
			List<MachineState> startStates,
			ConcurrentHashMap<MachineState, DCScore> c) {
		super(sm, ourPlayer, startStates, c);
	}

	@Override
	public void run_charges() {
		while(!Thread.interrupted()){
			for(MachineState s : startStates) {
				try {
					scores.get(s).addScore(sm.getGoal(sm.performDepthCharge(s, null),ourPlayer));
				} catch (TransitionDefinitionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (MoveDefinitionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (GoalDefinitionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}		
	}

}
