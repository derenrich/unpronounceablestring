package util.depthcharger;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import player.gamer.exception.MoveSelectionException;

import util.logging.GamerLogger;
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
		int charge_count = 0;
		while(!Thread.interrupted()){
			for(MachineState s : startStates) {
				try {
					if(!Thread.currentThread().isInterrupted()) {
						charge_count++;
						scores.get(s).addScore(sm.getGoal(sm.performDepthCharge(s, null), ourPlayer));
					} 
				} catch (TransitionDefinitionException e) {
					e.printStackTrace();
				} catch (MoveDefinitionException e) {
					e.printStackTrace();
				} catch (GoalDefinitionException e) {
					e.printStackTrace();
				}	catch (Exception e)
				{
				    GamerLogger.logStackTrace("GamePlayer", e);
				}

			}
		}		
		charge_count++;
	}

}
