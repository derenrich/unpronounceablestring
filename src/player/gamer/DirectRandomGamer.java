package player.gamer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import player.gamer.event.GamerNewMatchEvent;
import player.gamer.exception.MetaGamingException;
import player.gamer.exception.MoveSelectionException;
import util.game.Game;
import util.gdl.grammar.GdlProposition;
import util.gdl.grammar.GdlSentence;
import util.match.Match;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import util.statemachine.implementation.prover.ProverStateMachine;

public class DirectRandomGamer extends Gamer {
	private Match match;
    private Role role;
    private MachineState currentState;
    private StateMachine stateMachine;          

	@Override
	public boolean start(String matchId, GdlProposition roleName, Game game,
			int startClock, int playClock, long receptionTime)
			throws MetaGamingException {	
		this.match = new Match(matchId, startClock, playClock, game);
			
		stateMachine = new ProverStateMachine();
		stateMachine.initialize(match.getGame().getRules());
		currentState = stateMachine.getInitialState();
		role = stateMachine.getRoleFromProp(roleName);
		match.appendState(currentState.getContents());
		
		return true;
	}

	@Override
	public GdlSentence play(String matchId, List<GdlSentence> moves,
			long receptionTime) throws MoveSelectionException,
			TransitionDefinitionException, MoveDefinitionException {
		match.appendMoves(moves);
		stateMachine.doPerMoveWork();
		
		List<GdlSentence> lastMoves = match.getMostRecentMoves();
		if (lastMoves != null)
		{
			List<Move> moves_list = new ArrayList<Move>();
			for (GdlSentence sentence : lastMoves)
			{
				moves_list.add(stateMachine.getMoveFromSentence(sentence));
			}

			currentState = stateMachine.getNextState(currentState, moves_list);
			match.appendState(currentState.getContents());
		}
		
		List<Move> legal_moves = stateMachine.getLegalMoves(currentState, role);
		Move selection = (legal_moves.get(new Random().nextInt(legal_moves.size())));
		return selection.getContents();


	}

	@Override
	public boolean stop(String matchId, List<GdlSentence> moves) {
		return true;
	}

	@Override
	public boolean ping() {
		return true;
	}

	@Override
	public boolean abort(String matchId) {
		return true;
	}

	@Override
	public Match getMatch(String matchId) {
		// TODO Auto-generated method stub
		return match;
	}

}
