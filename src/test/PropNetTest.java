package test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import util.gdl.grammar.Gdl;
import util.gdl.grammar.GdlConstant;
import util.gdl.grammar.GdlPool;
import util.gdl.grammar.GdlProposition;
import util.gdl.grammar.GdlTerm;
import util.kif.KifReader;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.implementation.propnet.PropNetStateMachine;
import util.statemachine.implementation.prover.ProverStateMachine;

public class PropNetTest {
	 protected final static PropNetStateMachine sm = new PropNetStateMachine();
	 public static void main(String args[]) throws Exception {
	        List<Gdl> ticTacToeDesc = KifReader.read("games/games/ticTacToe/ticTacToe.kif");
	        sm.initialize(ticTacToeDesc);
	        MachineState state = sm.getInitialState();
	        Assert.assertFalse(sm.isTerminal(state));
	        GdlConstant X_PLAYER = GdlPool.getConstant("xplayer");
	        GdlConstant O_PLAYER = GdlPool.getConstant("oplayer");
	        GdlProposition X_PLAYER_P = GdlPool.getProposition(X_PLAYER);
	        GdlProposition O_PLAYER_P = GdlPool.getProposition(O_PLAYER);
	        Role xRole = new Role(X_PLAYER_P);
	        Role oRole = new Role(O_PLAYER_P);
	        List<Role> roles = Arrays.asList(xRole, oRole);
	        Assert.assertEquals(sm.getRoles(), roles);

	        Assert.assertEquals(sm.getLegalJointMoves(state).size(), 9);
	        Assert.assertEquals(sm.getLegalMoves(state, xRole).size(), 9);
	        Assert.assertEquals(sm.getLegalMoves(state, oRole).size(), 1);
	        Move noop = new Move(GdlPool.getProposition(GdlPool.getConstant("noop")));
	        Assert.assertEquals(sm.getLegalMoves(state, oRole).get(0), noop);

	        Move m11 = move("play 1 1 x");
	        Assert.assertTrue(sm.getLegalMoves(state, xRole).contains(m11));
	        state = sm.getNextState(state, Arrays.asList(new Move[] {m11, noop}));
	        Assert.assertFalse(sm.isTerminal(state));

	        Move m13 = move("play 1 3 o");
	        Assert.assertTrue(sm.getLegalMoves(state, oRole).contains(m13));
	        state = sm.getNextState(state, Arrays.asList(new Move[] {noop, m13}));
	        Assert.assertFalse(sm.isTerminal(state));

	        Move m31 = move("play 3 1 x");
	        Assert.assertTrue(sm.getLegalMoves(state, xRole).contains(m31));
	        state = sm.getNextState(state, Arrays.asList(new Move[] {m31, noop}));
	        Assert.assertFalse(sm.isTerminal(state));

	        Move m22 = move("play 2 2 o");
	        Assert.assertTrue(sm.getLegalMoves(state, oRole).contains(m22));
	        state = sm.getNextState(state, Arrays.asList(new Move[] {noop, m22}));
	        Assert.assertFalse(sm.isTerminal(state));

	        Move m21 = move("play 2 1 x");
	        Assert.assertTrue(sm.getLegalMoves(state, xRole).contains(m21));
	        state = sm.getNextState(state, Arrays.asList(new Move[] {m21, noop}));
	        Assert.assertTrue(sm.isTerminal(state));
	        Assert.assertEquals(sm.getGoal(state, xRole), 100);
	        Assert.assertEquals(sm.getGoal(state, oRole), 0);
	        Assert.assertEquals(sm.getGoals(state), Arrays.asList(new Integer[] {100, 0}));

	        //My expectations for the behavior, but there's no consensus...
	        /*Move m23 = new Move(GdlPool.getRelation(PLAY, new GdlTerm[] {C2, C3, O}));
	        try {
	            sm.getNextState(state, Arrays.asList(new Move[] {noop, m23}));
	            Assert.fail("Should throw an exception when trying to transition from a terminal state");
	        } catch(TransitionDefinitionException e) {
	            //Expected
	        }*/
	    }
	 protected static Move move(String description) {
	        String[] parts = description.split(" ");
	        GdlConstant head = GdlPool.getConstant(parts[0]);
	        if(parts.length == 1)
	            return new Move(GdlPool.getProposition(head));
	        List<GdlTerm> body = new ArrayList<GdlTerm>();
	        for(int i = 1; i < parts.length; i++) {
	            body.add(GdlPool.getConstant(parts[i]));
	        }
	        return new Move(GdlPool.getRelation(head, body));
	    }
}
