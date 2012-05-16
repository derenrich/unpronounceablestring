package util.statemachine.implementation.propnet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import util.gdl.grammar.Gdl;
import util.gdl.grammar.GdlConstant;
import util.gdl.grammar.GdlProposition;
import util.gdl.grammar.GdlRelation;
import util.gdl.grammar.GdlSentence;
import util.gdl.grammar.GdlTerm;
import util.propnet.architecture.Component;
import util.propnet.architecture.PropNet;
import util.propnet.architecture.components.And;
import util.propnet.architecture.components.Proposition;
import util.propnet.architecture.components.Transition;
import util.propnet.factory.OptimizingPropNetFactory;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import util.statemachine.implementation.prover.query.ProverQueryBuilder;

@SuppressWarnings("unused")
public class SamplePropNetStateMachine extends StateMachine {
    /** The underlying proposition network  */
    private PropNet propNet;
    /** The topological ordering of the propositions */
    private List<Proposition> ordering;
    /** The player roles */
    private List<Role> roles;
    
    /**
     * Initializes the PropNetStateMachine. You should compute the topological
     * ordering here. Additionally you may compute the initial state here, at
     * your discretion.
     */
    @Override
    public void initialize(List<Gdl> description) {
    	System.out.println("Test");
        try {
			propNet = OptimizingPropNetFactory.create(description);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        roles = propNet.getRoles();
        // Debug:
        System.out.println("Links: "+propNet.getNumLinks());
        System.out.println("bp: "+propNet.getBasePropositions().size());
        System.out.println("bp: "+propNet.getBasePropositions().values().size());
        System.out.println("Inputs: "+propNet.getInputPropositions().values().size());
        System.out.println("Inputs: "+propNet.getInputPropositions().values());
        propNet.renderToFile("debug.txt");
        ordering = getOrdering();
    }    
    
	/**
	 * Computes if the state is terminal. Should return the value
	 * of the terminal proposition for the state.
	 */
	@Override
	public boolean isTerminal(MachineState state) {
		// TODO: Compute whether the MachineState is terminal.
		
		//How do I get state into the propnet?
		for(Proposition p : ordering) {
			/*if(p==propNet.getTerminalProposition())
				return p.getValue();
			else
				p.getValue();*/
		}
		
		return propNet.getTerminalProposition().getValue();
	}
	
	/**
	 * Computes the goal for a role in the current state.
	 * Should return the value of the goal proposition that
	 * is true for that role. If there is not exactly one goal
	 * proposition true for that role, then you should throw a
	 * GoalDefinitionException because the goal is ill-defined. 
	 */
	@Override
	public int getGoal(MachineState state, Role role)
	throws GoalDefinitionException {
		// TODO: Compute the goal for role in state.
		//getGoalValue()
		return -1;
	}
	
	/**
	 * Returns the initial state. The initial state can be computed
	 * by only setting the truth value of the INIT proposition to true,
	 * and then computing the resulting state.
	 */
	@Override
	public MachineState getInitialState() {
		// TODO: Compute the initial state.
		propNet.getInitProposition().setValue(true);
		
		// To compute t
		
		return null;
	}
	
	/**
	 * Computes the legal moves for role in state.
	 */
	@Override
	public List<Move> getLegalMoves(MachineState state, Role role)
	throws MoveDefinitionException {
		// TODO: Compute legal moves.
		return null;
	}
	
	/**
	 * Computes the next state given state and the list of moves.
	 */
	@Override
	public MachineState getNextState(MachineState state, List<Move> moves)
	throws TransitionDefinitionException {
		// TODO: Compute the next state.
		List<GdlTerm> terms = toDoes(moves);
		return null;
	}
	
	/**
	 * This should compute the topological ordering of propositions.
	 * Each component is either a proposition, logical gate, or transition.
	 * Logical gates and transitions only have propositions as inputs.
	 * 
	 * The base propositions and input propositions should always be exempt
	 * from this ordering.
	 * 
	 * The base propositions values are set from the MachineState that
	 * operations are performed on and the input propositions are set from
	 * the Moves that operations are performed on as well (if any).
	 * 
	 * @return The order in which the truth values of propositions need to be set.
	 */
	public List<Proposition> getOrdering()
	{
	    // List to contain the topological ordering.
	    List<Proposition> order = new LinkedList<Proposition>();
	    				
		// All of the components in the PropNet
		HashSet<Component> components = new HashSet<Component>(propNet.getComponents());
		
		// All of the propositions in the PropNet.
		List<Proposition> propositions = new ArrayList<Proposition>(propNet.getPropositions());
		
		// TODO: Compute the topological ordering.	
		
		// Generate list of starting nodes
		List<Proposition> start = new ArrayList<Proposition>();
		start.addAll(propNet.getInputPropositions().values());
		start.addAll(propNet.getBasePropositions().values());
		start.add(propNet.getInitProposition());

		// Have we already added the Component to the ordering?
		Set<Component> used = new HashSet<Component>();
		
		Set<Component> frontier = new HashSet<Component>();
		Set<Component> fringe;
		for(Component s :start) {
			frontier.addAll(s.getOutputs());
		}
		used.addAll(start);
		// Perform Depth first search of sorts
		while(!frontier.isEmpty()){
			// Add what we can to the ordering
			Iterator<Component> iter = frontier.iterator();
			fringe = new HashSet<Component>();
			while(iter.hasNext()) {
				Component c = iter.next();
				boolean addMe = true;
				for(Component parent:c.getInputs()) {
					if(!used.contains(parent))
						addMe = false;
				}
				if(addMe) {
					iter.remove();
					if(c instanceof Proposition)
						order.add((Proposition)c);
					used.add(c);
					// Expand the frontier
					fringe.addAll(c.getOutputs());
				}
			}
			
			// Expand the frontier

			// baseProps are problematic. This is taken care of by not expanding transitions
			//newFringe.removeAll(baseProps);
			fringe.removeAll(used);
			// System.out.println("Adding : "+fringe.size());
			// System.out.println("Frontier size: " + frontier.size());
			// System.out.println("Ordered : "+order.size()+" of " + (propositions.size()-start.size()));
			frontier.addAll(fringe);
			if(fringe.size()==0) {
				System.out.println("We have run out of options");
				propositions.removeAll(order);
				propositions.removeAll(start);
				System.out.println("The "+propositions.size()+" lost souls: "+propositions);
				break;
			}
		}
		return order;
	}

	/* Already implemented for you */
	@Override
	public List<Role> getRoles() {
		return roles;
	}

	/* Helper methods */
	
	/**
	 * Does the stuffz
	 */
	private void propogateTruth(){
		for(Proposition p : ordering) {
			p.setValue(p.getSingleInput().getValue());
		}
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
	
	/**
	 * Takes in a Legal Proposition and returns the appropriate corresponding Move
	 * @param p
	 * @return a PropNetMove
	 */
	public static Move getMoveFromProposition(Proposition p)
	{
		return new Move(p.getName().toSentence().get(1).toSentence());
	}
	
	/**
	 * Helper method for parsing the value of a goal proposition
	 * @param goalProposition
	 * @return the integer value of the goal proposition
	 */	
    private int getGoalValue(Proposition goalProposition)
	{
		GdlRelation relation = (GdlRelation) goalProposition.getName().toSentence();
		GdlConstant constant = (GdlConstant) relation.get(1);
		return Integer.parseInt(constant.toString());
	}
	
	/**
	 * A Naive implementation that computes a PropNetMachineState
	 * from the true BasePropositions.  This is correct but slower than more advanced implementations
	 * You need not use this method!
	 * @return PropNetMachineState
	 */	
	public MachineState getStateFromBase()
	{
		Set<GdlSentence> contents = new HashSet<GdlSentence>();
		for (Proposition p : propNet.getBasePropositions().values())
		{
			p.setValue(p.getSingleInput().getValue());
			if (p.getValue())
			{
				contents.add(p.getName().toSentence());
			}

		}
		return new MachineState(contents);
	}
}