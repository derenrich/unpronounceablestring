package util.propnet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import util.propnet.architecture.Component;
import util.propnet.architecture.PropNet;
import util.propnet.architecture.components.*;


public class PropNetAnalysis {
	private PropNet propnet;
	
	public PropNetAnalysis(PropNet propnet) {
		this.propnet = propnet;
	}
	
	public List<Proposition> detectLatches() {
		List<Proposition> latches = new ArrayList<Proposition>();		
		for(Proposition prop : propnet.getBasePropositions().values()) {
			Set<Proposition> impliedTrue = new HashSet<Proposition>();
			Set<Component> transitionedComponents = new HashSet<Component>(); 
			Set<Component> deferredExplore = new HashSet<Component>();
			Stack<Component> toExplore = new Stack<Component>();
			Set<Component> seen = new HashSet<Component>();
			toExplore.addAll(prop.getOutputs());
			while(toExplore.size() > 0) {
				if(impliedTrue.contains(prop)) {
					latches.add(prop);
					break;
				}
				Component next = toExplore.pop();
				if (next instanceof Proposition) {
					impliedTrue.add((Proposition)next);					
				}
				// we do not explore through Not's or Constant's
				if (next instanceof Proposition ||
					next instanceof Or ) {
					if(!seen.contains(next)) {
						toExplore.addAll(next.getOutputs());
					}
					if(transitionedComponents.contains(next)) {
						transitionedComponents.addAll(next.getOutputs());
					}
				}
				if (next instanceof Transition &&
					! transitionedComponents.contains(next)) {
					transitionedComponents.add(next);
					if(!seen.contains(next)) {
						toExplore.addAll(next.getOutputs());
						transitionedComponents.addAll(next.getOutputs());
					}
				}
				if (next instanceof And) {
					deferredExplore.add(next);
				}
				seen.add(next);
			}
		}
		return latches;
	}
}
