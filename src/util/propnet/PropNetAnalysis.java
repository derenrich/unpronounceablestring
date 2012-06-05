package util.propnet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import util.gdl.grammar.GdlSentence;
import util.propnet.architecture.Component;
import util.propnet.architecture.PropNet;
import util.propnet.architecture.components.*;
import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.implementation.propnet.PropNetStateMachine;


public class PropNetAnalysis {
	private PropNet propnet;
	private List<Proposition> latches;
	private Map<Proposition,Set<Proposition>> reqs;
	private Map<Proposition,Set<Proposition>> antireqs;
	private Map<Proposition,Set<Proposition>> inhibitions;
	private static final int CUTOFF_SIZE = 15000;
	
	
	private Map<Role, Integer> bestGoals;
	public PropNetAnalysis(PropNet propnet) {
		this.propnet = propnet;
		this.latches = detectLatches();
		this.reqs = detectRequirements();
		this.antireqs = detectAntirequirements();
		this.inhibitions = detectInhibitions();		
		List<Proposition> useful_latches = new ArrayList<Proposition>();
		for(Proposition p : this.latches) {
			boolean useful = false;
			for(Role r : this.propnet.getRoles()) {
				if(useful)
					break;
				Set<Proposition> goals = propnet.getGoalPropositions().get(r);
				for(Proposition g : goals) {
					if(useful)
						break;
					if(reqs.get(p).contains(g) || inhibitions.get(p).contains(g)) {
						useful = true;
					}					
				}
			}
			if(useful) {
				useful_latches.add(p);
			}
		}
		this.latches = useful_latches;
		this.bestGoals = new HashMap<Role, Integer>();
		for(Role r : this.propnet.getRoles()) {
			int max_goal = 100;
			for(Proposition g : this.propnet.getGoalPropositions().get(r)) {
				max_goal = Math.max(max_goal, PropNetStateMachine.getGoalValue(g));
			}
			this.bestGoals.put(r, max_goal);
		}
		System.out.println("Latch count: " + latches.size());
		
	}
	/*
	 * What is the most this state could be worth as a fraction of the best
	 */
	public float maxValue(MachineState state, Role r) {
		Set<Proposition> props = new HashSet<Proposition>();
		for(GdlSentence s : state.getContents()) {
			if(propnet.getBasePropositions().containsKey(s.toTerm())) {
				props.add(propnet.getBasePropositions().get(s.toTerm()));
			}
		}
		Set<Proposition> goals = propnet.getGoalPropositions().get(r);
		Set<Proposition> inhibited_goals = new HashSet<Proposition>(); 
		for(Proposition p : props) {
			if(this.latches.contains(p)) {				
				for(Proposition g : goals) {
					if(reqs.get(p).contains(g)) {
						// one of the goals is required so we're done here
						return (PropNetStateMachine.getGoalValue(g) / (float) this.bestGoals.get(r));
					}
					if(!inhibitions.get(p).contains(g)) {
						inhibited_goals.add(g);
					}
				}
			}
		}
		goals.removeAll(inhibited_goals);
		int best_available_goal = 0;
		for(Proposition g : goals) {
			if(inhibited_goals.contains(g)) {
				continue;
			}
			best_available_goal  = Math.max(best_available_goal , PropNetStateMachine.getGoalValue(g));
		}
		return best_available_goal / (float) this.bestGoals.get(r);
	}
	
	private void utility() {
		int useful_count = 0;
		for(Role r : propnet.getRoles()) {
			Set<Proposition> goals = propnet.getGoalPropositions().get(r);
			for(Proposition p : this.latches) {				
				for(Proposition g : goals) {
					if(reqs.get(p).contains(g)) {
						useful_count ++;
					}	
					if(!inhibitions.get(p).contains(g)) {
						useful_count ++;
					}
				}

			}
		}
		System.out.println("Useful count : " + useful_count);
	}
	
	private List<Proposition> detectLatches() {
		List<Proposition> latches = new ArrayList<Proposition>();		
		boolean breakOut = false;
		for(Proposition prop : propnet.getBasePropositions().values()) {
			Set<Proposition> impliedTrue = new HashSet<Proposition>();
			Set<Proposition> impliedFalse = null;
			Set<Component> transitionedComponents = new HashSet<Component>();
			Set<Component> negatedComponents = new HashSet<Component>(); 
			Stack<Component> toExplore = new Stack<Component>();
			Set<Component> seen = new HashSet<Component>();
			toExplore.addAll(prop.getOutputs());
			while(toExplore.size() > 0) {
				Component next = toExplore.pop();
				explore(next, toExplore, impliedTrue, null, transitionedComponents, negatedComponents, seen, true);
				if(toExplore.size() > CUTOFF_SIZE ) {
					breakOut = true;
					break;
				}
				if(impliedTrue.contains(prop)) {
					latches.add(prop);
					break;
				}				
			}
			if (breakOut)
				break;
		}
		return latches;
	}
	private void explore(Component next, Stack<Component> toExplore,
				Set<Proposition> impliedTrue,
				Set<Proposition> impliedFalse,
				Set<Component> transitionedComponents,
				Set<Component> negatedComponents,
				Set<Component> seen,
				boolean transition){ 
		if (seen.contains(next) || next instanceof Constant)
			return;
		if(!transition && next instanceof Transition)
			return;
		if (next instanceof Transition && transitionedComponents.contains(next))
			return;
		if (negatedComponents.contains(next)) {
			if (impliedFalse != null && next instanceof Proposition) {
				impliedFalse.add((Proposition)next);
			}
			if (transitionedComponents.contains(next) || next instanceof Transition) {
				transitionedComponents.addAll(next.getOutputs());
			}
			if (!(next instanceof Not || next instanceof Or)) {
				negatedComponents.addAll(next.getOutputs());
			}
		} else {
			if (impliedTrue != null && next instanceof Proposition) {
				impliedTrue.add((Proposition)next);					
			}
			if(transitionedComponents.contains(next) || next instanceof Transition) {
				transitionedComponents.addAll(next.getOutputs());
			}
			if (next instanceof Not || next instanceof And) {
				negatedComponents.addAll(next.getOutputs());
			}
		}
		toExplore.addAll(next.getOutputs());
		seen.add(next);
		
	}
	/*
	 * Antirequiement: q requires p iff q implies p
	 */	
	private Map<Proposition,Set<Proposition>> detectRequirements() {
		Map<Proposition, Set<Proposition>> requirements = new HashMap<Proposition, Set<Proposition>>();		
		boolean breakOut = false;
		for(Proposition prop : propnet.getBasePropositions().values()) {
			Set<Proposition> impliedTrue = new HashSet<Proposition>();
			Set<Proposition> impliedFalse = new HashSet<Proposition>();
			Set<Component> transitionedComponents = new HashSet<Component>();
			Set<Component> negatedComponents = new HashSet<Component>(); 
			Stack<Component> toExplore = new Stack<Component>();
			Set<Component> seen = new HashSet<Component>();
			toExplore.add(prop);
			negatedComponents.add(prop);
			while(toExplore.size() > 0) {
				Component next = toExplore.pop();
				if(toExplore.size() > CUTOFF_SIZE ) {
					breakOut = true;
					break;
				}

				explore(next, 
						toExplore,
						impliedTrue,
						impliedFalse,
						transitionedComponents,
						negatedComponents, seen,
						/* do not pass through transitions */ false);				
			}
			if(breakOut)
				break;
			impliedFalse.remove(prop);
			requirements.put(prop, impliedFalse);
		}
		return requirements;
	}
	
	/*
	 * Antirequiement: q anti-requires p iff q implies not p
	 */
	private Map<Proposition,Set<Proposition>> detectAntirequirements() {
		Map<Proposition, Set<Proposition>> antirequirements = new HashMap<Proposition, Set<Proposition>>();
		boolean breakOut = false;
		for(Proposition prop : propnet.getBasePropositions().values()) {
			Set<Proposition> impliedTrue = new HashSet<Proposition>();
			Set<Proposition> impliedFalse = new HashSet<Proposition>();
			Set<Component> transitionedComponents = new HashSet<Component>();
			Set<Component> negatedComponents = new HashSet<Component>(); 
			Stack<Component> toExplore = new Stack<Component>();
			Set<Component> seen = new HashSet<Component>();
			toExplore.add(prop);
			while(toExplore.size() > 0) {
				Component next = toExplore.pop();
				if(toExplore.size() > CUTOFF_SIZE ) {
					breakOut = true;
					break;
				}
				
				explore(next, 
						toExplore,
						impliedTrue,
						impliedFalse,
						transitionedComponents,
						negatedComponents, seen,
						/* do not pass through transitions */ false);				
			}
			impliedFalse.remove(prop);
			impliedTrue.remove(prop);
			antirequirements.put(prop, impliedFalse);
			if(breakOut)
				break;
		}
		return antirequirements;
	}
	/*
	 * Inhibitions: p inhibits q iff not p implies not q (now or in the next state)
	 */	
	private Map<Proposition,Set<Proposition>> detectInhibitions() {
		Map<Proposition, Set<Proposition>> inhibitions = new HashMap<Proposition, Set<Proposition>>();
		boolean breakOut = false;
		for(Proposition prop : propnet.getBasePropositions().values()) {
			Set<Proposition> impliedTrue = new HashSet<Proposition>();
			Set<Proposition> impliedFalse = new HashSet<Proposition>();
			Set<Component> transitionedComponents = new HashSet<Component>();
			Set<Component> negatedComponents = new HashSet<Component>(); 
			Stack<Component> toExplore = new Stack<Component>();
			Set<Component> seen = new HashSet<Component>();
			toExplore.add(prop);
			while(toExplore.size() > 0 && !breakOut) {
				Component next = toExplore.pop();
				if(toExplore.size() > CUTOFF_SIZE ) {
					breakOut = true;
				}				
				explore(next, 
						toExplore,
						impliedTrue,
						impliedFalse,
						transitionedComponents,
						negatedComponents, seen,
						true);				
			}
			
			impliedFalse.remove(prop);
			inhibitions.put(prop, impliedFalse);
			if(breakOut)
				break;
		}
		return inhibitions;
	}

}
