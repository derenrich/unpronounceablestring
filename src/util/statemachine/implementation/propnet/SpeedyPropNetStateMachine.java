package util.statemachine.implementation.propnet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import util.gdl.grammar.Gdl;
import util.gdl.grammar.GdlSentence;
import util.gdl.grammar.GdlTerm;
import util.propnet.architecture.Component;
import util.propnet.architecture.components.And;
import util.propnet.architecture.components.Constant;
import util.propnet.architecture.components.Not;
import util.propnet.architecture.components.Or;
import util.propnet.architecture.components.Proposition;
import util.propnet.architecture.components.Transition;

import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import util.statemachine.implementation.prover.query.ProverQueryBuilder;

public class SpeedyPropNetStateMachine extends StateMachine {

	private final Map<GdlTerm, Integer> basePropositions;
	private final Map<GdlTerm, Integer> inputPropositions;
	private final Map<Role, Set<Integer>> legalPropositions;
	private final Map<Role, Set<Integer>> goalPropositions;
	private Integer initProposition;
	private Integer terminalProposition;
    private List<Role> roles;
  

	
	public SpeedyPropNetStateMachine(PropNetStateMachine pnsm){
		basePropositions = new HashMap<GdlTerm, Integer>();
		inputPropositions = new HashMap<GdlTerm, Integer>();
		legalPropositions = new HashMap<Role, Set<Integer>>();
		goalPropositions = new  HashMap<Role, Set<Integer>>();
    	roles = pnsm.getRoles();
		this.compilePropNet(pnsm);
	}
	
	@Override
	public void initialize(List<Gdl> description) {
		PropNetStateMachine pnsm = new PropNetStateMachine();
		pnsm.initialize(description);
		
	}

	enum ComponentTypes {OrType, NotType, AndType, PropositionType, TransitionType, ConstantType};
	
	// contains all the initialized values of the constants
	private boolean base_state_values[];
	private int num_components;
	private int num_propositions;

	private ComponentTypes types[];
	private Map<Integer, Component> id_to_component;
	private Map<Component, Integer> component_to_id;
	ArrayList<Integer> outputs[];
	ArrayList<Integer> inputs[];

	ArrayList<Integer> ordering;
		
	private void compilePropNet(PropNetStateMachine pnsm) {	
		Set<Component> components = pnsm.propNet.getComponents();
		id_to_component = new HashMap<Integer, Component>();
		component_to_id = new HashMap<Component, Integer>();
		int id = 0;
		for(Component c : pnsm.propNet.getPropositions()) {
			id_to_component.put(id, c);
			component_to_id.put(c, id);
			id++;
		}
		for(Component c : pnsm.propNet.getConstantComponents()) {
			id_to_component.put(id, c);
			component_to_id.put(c, id);
			id++;
		}		
		num_propositions = id;
		for(Component c : components) {
			if(c instanceof Proposition || c instanceof Constant) {
				// we already did them
				// we do this to give them the first ids
				continue;
			}
			id_to_component.put(id, c);
			component_to_id.put(c, id);
			id++;
		}
		
		num_components = id_to_component.size();
		base_state_values = new boolean[num_propositions];
		types = new ComponentTypes[num_components];

		outputs = new ArrayList[num_components];
		inputs = new ArrayList[num_components];
		// populate our stuff
		for(Integer i : id_to_component.keySet()) {
			Component c = id_to_component.get(i);			
			if(c instanceof Constant) {
				base_state_values[i] = c.getValue();
			}			
			outputs[i] = new ArrayList<Integer>();
			inputs[i] = new ArrayList<Integer>();
			for(Component out : c.getOutputs()) {
				outputs[i].add(component_to_id.get(out));
			}
			for(Component ins : c.getInputs()) {
				inputs[i].add(component_to_id.get(ins));
			}
			if(c instanceof And) {
				types[i] = ComponentTypes.AndType;
			} else if(c instanceof Constant) {
				types[i] = ComponentTypes.ConstantType;
			} else if(c instanceof Not) {
				types[i] = ComponentTypes.NotType;
			} else if(c instanceof Or) {
				types[i] = ComponentTypes.OrType;
			} else if(c instanceof Proposition) {
				types[i] = ComponentTypes.PropositionType;
			} else if(c instanceof Transition) {
				types[i] = ComponentTypes.TransitionType;
			}
		}
		ordering = new ArrayList<Integer>();
		for(Component c : pnsm.ordering) {
			ordering.add(component_to_id.get(c));
		}
		for(GdlTerm t : pnsm.propNet.getBasePropositions().keySet()) {
			Proposition p = pnsm.propNet.getBasePropositions().get(t);
			basePropositions.put(t, component_to_id.get(p));
		}
		for(GdlTerm t : pnsm.propNet.getInputPropositions().keySet()) {
			Proposition p = pnsm.propNet.getInputPropositions().get(t);
			inputPropositions.put(t, component_to_id.get(p));
		}
		for(Role r : pnsm.propNet.getLegalPropositions().keySet()) {
			Set<Proposition> props = pnsm.propNet.getLegalPropositions().get(r);
			Set<Integer> id_props = new HashSet<Integer>();
			for(Proposition p : props) {
				id_props.add(component_to_id.get(p));				
			}
			legalPropositions.put(r, id_props);
		}
		for(Role r : pnsm.propNet.getGoalPropositions().keySet()) {
			Set<Proposition> props = pnsm.propNet.getGoalPropositions().get(r);
			Set<Integer> id_props = new HashSet<Integer>();
			for(Proposition p : props) {
				id_props.add(component_to_id.get(p));				
			}
			goalPropositions.put(r, id_props);
		}
		initProposition = component_to_id.get(pnsm.propNet.getInitProposition());
		terminalProposition = component_to_id.get(pnsm.propNet.getTerminalProposition());
	}
	
	@Override
	public int getGoal(MachineState state, Role role)
			throws GoalDefinitionException {
		boolean state_values[] = base_state_values.clone();
    	this.injectState(state, state_values);
    	this.propogateTruth(state_values);
		
    	for(int pidx : goalPropositions.get(role)){
    		if(state_values[pidx]) {
    			return PropNetStateMachine.getGoalValue((Proposition) id_to_component.get(pidx));
    		}
    	}
    	return -1;
	}

	@Override
	public boolean isTerminal(MachineState state) {
		boolean state_values[] = base_state_values.clone();
    	this.injectState(state, state_values);
    	this.propogateTruth(state_values);
    	return state_values[terminalProposition];
	}

	@Override
	public List<Role> getRoles() {
		return roles;
	}

	private void injectState(MachineState state, boolean states[]) {
		// we assume this started out in a fresh state
		for(GdlSentence s : state.getContents()) {
			if(basePropositions.containsKey(s.toTerm())) {
				int idx = basePropositions.get(s.toTerm());
				states[idx] = true;
			}
		}        
	}
	private void propogateTruth(boolean states[]){
		for(int pidx : ordering) {
			boolean val = search_values(inputs[pidx].get(0), states);
			states[pidx] = val;
		}
	}
	private boolean search_values(int idx, boolean states[]) {
		ArrayList<Integer> idx_inputs = inputs[idx];
		boolean state;
		int single_in;
		if(types == null || types[idx] ==null) {
			System.out.println("foo");
		}
		switch(types[idx]) {
		case OrType:
			state = false;
			for(int in : idx_inputs) {
				state |= search_values(in, states);
				if(state) {
					return state;
				}
			}
			return state;			
		case AndType:
			state = true;
			for(int in : idx_inputs) {
				state &= search_values(in, states);
				if(!state) {
					return state;
				}
			}
			return state;			
		case NotType:
			single_in = idx_inputs.get(0);
			return !search_values(single_in, states);
		case PropositionType:
		case ConstantType:			
			return states[idx];
		case TransitionType:
			single_in = idx_inputs.get(0);
			return search_values(single_in, states);
		default:
			System.out.println("THIS SHOULD NEVER HAPPEN GUYS");
			break;
		}
		// NEVER HAPPENS		
		return false;
	}
	
	
	@Override
	public MachineState getInitialState() {
		boolean state_values[] = base_state_values.clone();
		state_values[initProposition] = true;
    	this.propogateTruth(state_values);		
    	MachineState s =  this.getStateFromBase(state_values);
    	return s;		
	}
	
	private MachineState getStateFromBase(boolean state_values[])
	{
		Set<GdlSentence> contents = new HashSet<GdlSentence>();
		for (int pidx : basePropositions.values())
		{
			// may hit transitions
			state_values[pidx] = search_values(inputs[pidx].get(0), state_values);
			if (state_values[pidx])
			{
				Proposition p = (Proposition) id_to_component.get(pidx);
				contents.add(p.getName().toSentence());
			}
		}
		return new MachineState(contents);
	}
	

	@Override
	public List<Move> getLegalMoves(MachineState state, Role role)
			throws MoveDefinitionException {
		boolean state_values[] = base_state_values.clone();
		injectState(state, state_values);
    	propogateTruth(state_values);
    	Set<Integer> legal_props = legalPropositions.get(role);
    	List<Move> moves = new ArrayList<Move>();
    	for(int pidx : legal_props) {
    		if(state_values[pidx]) {
    			moves.add(PropNetStateMachine.getMoveFromProposition((Proposition) id_to_component.get(pidx)));
    		}
    	}    	
    	return moves;

	}

	@Override
	public MachineState getNextState(MachineState state, List<Move> moves)
			throws TransitionDefinitionException {
		boolean state_values[] = base_state_values.clone();
    	this.injectState(state,state_values);
    	List<GdlTerm> terms = toDoes(moves);
    	for(GdlTerm t : terms) {
    		if(inputPropositions.containsKey(t)) {
    			int idx = inputPropositions.get(t);
    			state_values[idx] = true;
    		}
    	}
    	this.propogateTruth(state_values);
    	MachineState s = this.getStateFromBase(state_values);
    	return s;
	}
	
	/**
	 * The Input propositions are indexed by (does ?player ?action).
	 * 
	 * This translates a list of Moves (backed by a sentence that is simply ?action)
	 * into GdlTerms that can be used to get Propositions from inputPropositions.
	 * and accordingly set their values etc.  This is a naive implementation when coupled with 
	 * setting input values, feel free to change this for a more efficient implementation.
	 * 
	 * @param moves
	 * @return
	 */
	private List<GdlTerm> toDoes(List<Move> moves)
	{
		List<GdlTerm> doeses = new ArrayList<GdlTerm>(moves.size());
		Map<Role, Integer> roleIndices = getRoleIndices();		
		for (int i = 0; i < roles.size(); i++)
		{
			int index = roleIndices.get(roles.get(i));
			doeses.add(ProverQueryBuilder.toDoes(roles.get(i), moves.get(index)).toTerm());
		}
		return doeses;
	}

}
