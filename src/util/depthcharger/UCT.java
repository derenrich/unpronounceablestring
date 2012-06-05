package util.depthcharger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import util.gdl.grammar.GdlSentence;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

public class UCT extends DepthCharger{
	public class Pair<L,R> {
		private final L left;
		private final R right;

		public Pair(L left, R right) {
			this.left = left;
			this.right = right;
		}

		public L getLeft() { return left; }
		public R getRight() { return right; }

		@Override
		public int hashCode() { return left.hashCode() ^ right.hashCode(); }

		@Override
		public boolean equals(Object o) {
			if (o == null) return false;
			if (!(o instanceof Pair)) return false;
			Pair<?, ?> pairo = (Pair<?, ?>) o;
			return this.left.equals(pairo.getLeft()) &&
		           this.right.equals(pairo.getRight());
		  }

		}
	public UCT(StateMachine sm, Role ourPlayer, List<MachineState> startStates,
			ConcurrentHashMap<MachineState, DCScore> c) {
		super(sm, ourPlayer, startStates, c);
	}

	@Override
	public void run_charges() {
		Random r = new Random();
		// What is the current estimate for a score of a state
		HashMap<MachineState, DCScore> cache = new HashMap<MachineState, DCScore>();
		// How many times have I had to make a choice at this node?
		HashMap<MachineState, Integer> t = new HashMap<MachineState, Integer>();
		// How many times have I chosen a move from a state
		HashMap<Pair<MachineState,MachineState>, Integer> s = new HashMap<Pair<MachineState,MachineState>, Integer>();
		double Cp = 1/Math.sqrt(2);
		double discount = 1;
		while(!Thread.interrupted()){
			for(MachineState start : startStates) {
				MachineState cur = start;
				try {
					ArrayList<MachineState> path = new ArrayList<MachineState>();
					while(!sm.isTerminal(cur)) {
						// Choose next move
						List<List<Move>> moves = sm.getLegalJointMoves(cur);
						double ucb, max=-1;
						HashSet<MachineState> maxIs = new HashSet<MachineState>();
						MachineState maxI = null;
						for(int i = 0; i<moves.size();i++) {
							MachineState next = sm.getNextState(cur, moves.get(i));
							if(!cache.containsKey(next)) {
								cache.put(next, new DCScore());
							}
							if(!t.containsKey(cur)) {
								t.put(cur, 1);
							}
							if(!s.containsKey(new Pair<MachineState,MachineState>(cur,next))) {
								s.put(new Pair<MachineState,MachineState>(cur,next), 1);
							}
							ucb = cache.get(next).value()+2*Cp*Math.sqrt(Math.log(t.get(cur))/s.get(new Pair<MachineState,MachineState>(cur,next)));
							if(ucb>max) {
								maxI = next;
								maxIs = new HashSet<MachineState>();
								maxIs.add(next);
								max = ucb;
							}else if(ucb==max) {
								maxIs.add(next);
							}
						}
						// Take Next move
						maxI = (MachineState)maxIs.toArray()[r.nextInt(maxIs.size())];
						path.add(maxI);
						t.put(cur, t.get(cur)+1);
						s.put(new Pair<MachineState, MachineState>(cur, maxI), s.get(new Pair<MachineState, MachineState>(cur, maxI))+1);
						cur = maxI;						
					}
					// Update the values along the path
					/* Get goal inbetween 0 and 100 */
					double goal = sm.getGoal(cur, ourPlayer)/100.;
					for(int i = path.size()-1; i >=0; i--){
						cache.get(path.get(i)).addScore(goal);
						goal *= discount;
					}
				} catch (MoveDefinitionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (TransitionDefinitionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (GoalDefinitionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				this.scores.put(start, cache.get(start));
			}
		}
	}

}

/*
 int nDepth = 0;
        while(!isTerminal(state) && !Thread.currentThread().isInterrupted()) {
            nDepth++;
            state = getNextStateDestructively(state, getRandomJointMove(state));
        }
 */
