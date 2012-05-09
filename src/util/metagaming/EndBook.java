package util.metagaming;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

public class EndBook {
	Set<MachineState> wins;
	Set<MachineState> losses;
	volatile MachineState start;
	StateMachine sm;
	Role r;
	final int THREAD_COUNT = 4;

	/* State must be concurrent see Collections.newSetFromMap(new ConcurrentHashMap<Object,Boolean>()) */
	public void generateBook(MachineState s, StateMachine sm, Role r, long timeout, Set<MachineState> wins, Set<MachineState> losses) throws InterruptedException {
		this.wins = wins;
		this.losses = losses;
		this.start = s;
		this.sm = sm;
		this.r = r;
		
		ArrayList<EndBookWriter> threads = new ArrayList<EndBookWriter>();
		for(int i = 0; i < THREAD_COUNT; i++){
			EndBookWriter t = new EndBookWriter();
			threads.add(t);
			t.start();
		}
		Thread.sleep(timeout - System.currentTimeMillis());
		for(Thread t : threads) {
			t.interrupt();
		}
	}
	public int bookSize() {
		return wins.size() + losses.size();
	}
	
	public void expandBook(MachineState s, StateMachine sm, Role r, Set<MachineState> wins, Set<MachineState> losses) throws InterruptedException {
		this.wins = wins;
		this.losses = losses;
		this.start = s;
		this.sm = sm;
		this.r = r;		
		EndBookWriter t = new EndBookWriter();
		t.setDaemon(true);
		t.start();
	}
	public void setState(MachineState s){
		this.start = s;
	}
	private class EndBookWriter extends Thread {
		final int MAX_DEPTH = 2;
		private int win(MachineState s) throws GoalDefinitionException {
			if(sm.getGoal(s, r) > 0) {
				return 1;
			} else{
				return -1;
			}
		}
		private void insertState(MachineState s, int score) {
			if(score == 1){
				wins.add(s);
			} else {
				losses.add(s);
			}			
		}
		private int checkWins(MachineState s) {
			if(wins.contains(s)){
				return 1;
			} else if (losses.contains(s)) {
				return -1;
			} else {
				return 0;
			}
		}
		
		private int Minimax(MachineState s, int depth) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
			if(sm.isTerminal(s)) {
				return win(s);
			} else if (checkWins(s) != 0) {
				return checkWins(s);
			} else if(depth == MAX_DEPTH) {
				return 0;
			} else {
				List<Move> our_moves = sm.getLegalMoves(s, r);
				List<Role> opposing_roles = new ArrayList<Role>(sm.getRoles());
				opposing_roles.remove(r);				
				int max_min_val = -1;
				for(Move our_move : our_moves) {
					int min_val = 1;
					List<List<Move>> joint_moves = sm.getLegalJointMoves(s, r, our_move);
					for(List<Move> joint_move : joint_moves) {
						MachineState next_state = sm.getNextState(s, joint_move);
						int val = Minimax(next_state, depth + 1);
						min_val = Math.min(val, min_val);
						// fake \alpha\beta pruning
						if(min_val == -1){
							break;
						}
					}
					max_min_val = Math.max(max_min_val, min_val);
				}
				if(max_min_val != 0) {
					insertState(s, max_min_val);
				}
				return max_min_val;
			}
		}
		public void run() {
			while(!this.isInterrupted()){
				try {
					ArrayList<MachineState> history = new ArrayList<MachineState>();
					MachineState final_state = sm.performRememberingDepthCharge(start, history);
					// what are the odds of hitting the same final state? who knows?
					if(!(checkWins(final_state) == 0)) {
						continue;
					}
					for(int i = history.size() - 1; i >= 0; i--) {
						MachineState s = history.get(i);						
						int score = Minimax(s, 0);					
						if (score == 0) {
							break;
						} else {
							insertState(s, score);
						}
					}
				} catch (TransitionDefinitionException e) {
					e.printStackTrace();
				} catch (MoveDefinitionException e) {
					e.printStackTrace();
				} catch (GoalDefinitionException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
