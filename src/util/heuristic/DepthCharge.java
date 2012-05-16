package util.heuristic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

public class DepthCharge extends Heuristic {
	final int THREAD_COUNT = 4;
	final int MAX_CHARGES = 4;
	final int MAX_TIME = 3000;
	ScheduledExecutorService e;
	CountDownLatch endSignal;
	
	public DepthCharge(StateMachine sm, Role ourPlayer) {
		super(sm, ourPlayer);
		e = Executors.newScheduledThreadPool(THREAD_COUNT);	
	}
	
	public int state_count = 0;
	public int total_charge_count = 0;
	
	ArrayList<Charger> threads;
	@Override
	public float getScore(MachineState s) throws MoveDefinitionException,
			TransitionDefinitionException, GoalDefinitionException {		
		long final_time = System.currentTimeMillis() + MAX_TIME;
		state_count++;
		List<Future<Integer>> results = new ArrayList<Future<Integer>>();
		for(int i=0; i < MAX_CHARGES; i++) {
			results.add(e.submit(new Charger(s)));
		}
		int goal_sum = 0;
		int charge_counts = 0;
		try {
			for(Future<Integer> f : results) {				
				try {						
					int goal = f.get(final_time - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
					if(goal >= 0) {
						goal_sum += goal;
						charge_counts++; 
					}
				} catch (ExecutionException e) {
					e.printStackTrace();
				} catch (TimeoutException e) {
					f.cancel(true);
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}
		total_charge_count += charge_counts;
		if (charge_counts > 0 ){
			return goal_sum / (float) charge_counts;
		} else {
			// assume the conservative option
			return 0;
		}
	}
	
	@Override
	public String getName() {
		return "DepthCharge";		
	}
	
	private class Charger implements Callable<Integer> {
		MachineState s;
		Charger(MachineState s){
			this.s = s;
		}
		@Override
		public Integer call() {
			try {
				MachineState final_state = sm.performDepthCharge(s, null);
				if (sm.isTerminal(final_state)){
					return sm.getGoal(final_state, ourPlayer);
				} else { 
					return -1;
				}
			} catch (GoalDefinitionException e) {
				e.printStackTrace();
				return 0;
			} catch (TransitionDefinitionException e) {
				e.printStackTrace();
				return 0;
			} catch (MoveDefinitionException e) {
				e.printStackTrace();
				return 0;
			}
		}
	}
}
