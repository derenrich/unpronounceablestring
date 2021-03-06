package player.gamer.statemachine.reflex.legal;

import java.util.List;

import player.gamer.statemachine.StateMachineGamer;
import player.gamer.statemachine.reflex.event.ReflexMoveSelectionEvent;
import player.gamer.statemachine.reflex.gui.ReflexDetailPanel;
import util.propnet.PropNetAnalysis;
import util.statemachine.Move;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import util.statemachine.implementation.propnet.PropNetStateMachine;
import util.statemachine.implementation.prover.ProverStateMachine;
import apps.player.detail.DetailPanel;

/**
 * LegalGamer is a very simple state-machine-based Gamer that will always
 * pick the first legal move that it finds at any state in the game. This
 * is one of a family of simple "reflex" gamers which act entirely on reflex
 * (picking the first legal move, or a random move) regardless of the current
 * state of the game.
 * 
 * This is not really a serious approach to playing games, and is included in
 * this package merely as an example of a functioning Gamer.
 */
public final class LegalGamer extends StateMachineGamer
{

	private PropNetAnalysis pna;
	/**
	 * Does nothing for the metagame
	 */
	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		// Do nothing.
	}
	
	/**
	 * Selects the first legal move
	 */
	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		long start = System.currentTimeMillis();

		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		Move selection = moves.get(0);
					
		long stop = System.currentTimeMillis();
		System.out.println(this.getCurrentState());
		//System.out.println(this.pna.maxValue(this.getCurrentState(), this.getRole()));
		notifyObservers(new ReflexMoveSelectionEvent(moves, selection, stop - start));
		return selection;
	}
	
	@Override
	public void stateMachineStop() {
		// Do nothing.
	}
	
	@Override
	public void stateMachineAbort() {
		// Do nothing
	}

	/**
	 * Uses a CachedProverStateMachine
	 */
	@Override
	public StateMachine getInitialStateMachine() {
		//return new ProverStateMachine();
		PropNetStateMachine sm = new PropNetStateMachine();		
		sm.initialize(this.match.getGame().getRules());
		pna = new PropNetAnalysis(sm.propNet);
		return sm;
	}
	@Override
	public String getName() {
		return "Legal";
	}

	@Override
	public DetailPanel getDetailPanel() {
		return new ReflexDetailPanel();
	}
}
