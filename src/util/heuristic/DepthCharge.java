package util.heuristic;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

public class DepthCharge extends Heuristic {
	final int THREAD_COUNT = 4;
	CountDownLatch endSignal;
	public DepthCharge(StateMachine sm, Role ourPlayer) {
		super(sm, ourPlayer);
		charge_wins = 0;			
		charge_losses = 0;		
		threads = new ArrayList<Charger>();
		for(int i = 0; i < THREAD_COUNT; i++){
			Charger t = new Charger();
			threads.add(t);
			 t.start();
		}
	}
	public int charge_count = 0;
	public int state_count = 0;
	
	volatile int charge_wins = 0;
	volatile int charge_losses = 0;
	ArrayList<Charger> threads;
	@Override
	public float getScore(MachineState s) throws MoveDefinitionException,
			TransitionDefinitionException, GoalDefinitionException {
		state_count++;
		charge_wins = 0;			
		charge_losses = 0;
		endSignal = new CountDownLatch(THREAD_COUNT);
		 for(Charger t : threads) {
			 synchronized(t){
				 t.ourPlayer = this.ourPlayer;
				 t.s = s;
				 t.suspended = false;
				 t.notify();
			 }
		 }
		// wait
		try {
			Thread.sleep(0,500);			
			for(Charger t : threads){
				t.suspended = true;
			}	
			endSignal.await();
		} catch (InterruptedException e) {
			// we need to finish ASAP
			// no time to join
			Thread.currentThread().interrupt();
		}
		if (charge_wins + charge_losses > 0) {
			charge_count += charge_wins + charge_losses;
			return charge_wins  / (float) (charge_wins + charge_losses);
		} else {
			// if we have to make something up...
			return 0.5f;
		}
	}
	@Override
	public String getName() {
		return "DepthCharge";
	}
	
	private class Charger extends Thread {
		public MachineState s;
		public Role ourPlayer;
		public boolean suspended = false;
		public  void win() {
			charge_wins++;
		}
		public void lose() {
			charge_losses++;
		}
		@Override
		public void run() {
			while(true){
			try {
				synchronized(this) {
					wait();
				}
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			while(!suspended) {
				try {
					MachineState final_state = sm.performDepthCharge(s, null);
					if(sm.getGoal(final_state, ourPlayer) > 0){
						win();
					} else {
						lose();
					}
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
			endSignal.countDown();
			}
		}
	}
}
