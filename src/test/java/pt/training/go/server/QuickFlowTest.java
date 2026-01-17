package pt.training.go.server;

import org.junit.jupiter.api.Test;
import pt.training.go.server.command.GameContext;
import pt.training.go.server.command.PlayerContext;
import pt.training.go.server.state.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class QuickFlowTest {

    static class FakePlayer implements PlayerContext {
        private final StoneColor color;
        private FakePlayer opponent;
        private final List<String> messages = new ArrayList<>();

        FakePlayer(StoneColor color) {
            this.color = color;
        }

        void setOpponent(FakePlayer opponent) {
            this.opponent = opponent;
        }

        List<String> getMessages() {
            return messages;
        }

        @Override
        public StoneColor getColor() {
            return color;
        }

        @Override
        public PlayerContext getOpponent() {
            return opponent;
        }

        @Override
        public void send(String line) {
            messages.add(line);
        }

        @Override
        public void requestStop() {
            // not needed for unit tests
        }
    }

    static class FakeGame implements GameContext {
        private final Board board;
        private GameState state;
        private FakePlayer currentPlayer;
        private FakePlayer blackPlayer;
        private FakePlayer whitePlayer;

        private int consecutivePasses = 0;

        private boolean[][] deadMarks;

        FakeGame(int size) {
            board = new Board(size);
            deadMarks = new boolean[size][size];
            state = new PlayingState();
        }

        void setPlayers(FakePlayer black, FakePlayer white) {
            blackPlayer = black;
            whitePlayer = white;
        }

        GameState getState() {
            return state;
        }

        FakePlayer getCurrentPlayer() {
            return currentPlayer;
        }

        @Override
        public Object lock() {
            return this;
        }

        @Override
        public void setState(GameState state) {
            this.state = state;
        }

        @Override
        public void setCurrentPlayer(PlayerContext player) {
            this.currentPlayer = (FakePlayer) player;
        }

        @Override
        public Board getBoard() {
            return board;
        }

        @Override
        public boolean isCurrentPlayer(PlayerContext player) {
            return player == currentPlayer;
        }

        @Override
        public void switchTurn() {
            if (currentPlayer != null && currentPlayer.opponent != null) {
                currentPlayer = currentPlayer.opponent;
                currentPlayer.send("YOUR_MOVE");
            }
        }

        @Override
        public void broadcast(String message) {
            if (blackPlayer != null) blackPlayer.send(message);
            if (whitePlayer != null) whitePlayer.send(message);
        }

        @Override
        public int getConsecutivePasses() {
            return consecutivePasses;
        }

        @Override
        public void resetPasses() {
            consecutivePasses = 0;
        }

        @Override
        public void incrementPasses() {
            consecutivePasses++;
        }

        @Override
        public boolean[][] getDeadMarks() {
            return deadMarks;
        }

        @Override
        public void clearDeadMarks() {
            for (int r = 0; r < deadMarks.length; r++) {
                for (int c = 0; c < deadMarks[r].length; c++) {
                    deadMarks[r][c] = false;
                }
            }
        }

        @Override
        public void toggleDead(int row, int col, PlayerContext player) {
            board.toggleDeadGroup(row, col, deadMarks);
        }

        @Override
        public void toggleDeadCommand(int row, int col, PlayerContext player) {
            state.toggleDead(this, player, row, col);
        }

        @Override
        public void applyDeadMarks() {
            board.applyDeadMarks(deadMarks);
        }

        @Override
        public void makeMove(int row, int col, PlayerContext player) {
            state.move(this, player, row, col);
        }

        @Override
        public void pass(PlayerContext player) {
            state.pass(this, player);
        }

        @Override
        public void requestResume(PlayerContext player) {
            state.requestResume(this, player);
        }

        @Override
        public void agreeEnd(PlayerContext player) {
            state.agreeEnd(this, player);
        }

        @Override
        public void resign(PlayerContext player) {
            state.resign(this, player);
        }

        @Override
        public void quit(PlayerContext player) {
            state.quit(this, player);
        }

        @Override
        public String boardFlat() {
            return board.toFlatString();
        }

        @Override
        public int getBoardSize() {
            return board.getSize();
        }
    }

    private FakeGame newGame5x5() {
        FakeGame game = new FakeGame(5);

        FakePlayer black = new FakePlayer(StoneColor.CZARNY);
        FakePlayer white = new FakePlayer(StoneColor.BIALY);
        black.setOpponent(white);
        white.setOpponent(black);

        game.setPlayers(black, white);
        game.setCurrentPlayer(black);

        return game;
    }

    @Test
    public void testPassPassGoesToScoring() {
        FakeGame game = newGame5x5();

        assertTrue(game.getState() instanceof PlayingState);

        FakePlayer black = game.blackPlayer;
        FakePlayer white = game.whitePlayer;

        // black pass
        game.pass(black);
        assertTrue(game.getState() instanceof PlayingState);
        assertEquals(1, game.getConsecutivePasses());

        // now white should be current (switchTurn in PlayingState.pass)
        // (if your implementation switches turn via game.switchTurn)
        // to be safe, we force currentPlayer in case you didn't auto-switch:
        if (game.getCurrentPlayer() == null) game.setCurrentPlayer(white);

        // white pass
        game.pass(white);

        assertTrue(game.getState() instanceof ScoringState);
        assertEquals(2, game.getConsecutivePasses());
    }

    @Test
    public void testRequestResumeRequesterMovesFirst() {
        FakeGame game = newGame5x5();

        FakePlayer black = game.blackPlayer;
        FakePlayer white = game.whitePlayer;

        // enter scoring: black pass, white pass
        game.pass(black);
        game.setCurrentPlayer(white);
        game.pass(white);

        assertTrue(game.getState() instanceof ScoringState);

        // requester = black
        game.requestResume(black);

        assertTrue(game.getState() instanceof PlayingState);
        assertEquals(black, game.getCurrentPlayer());

        // black should get YOUR_MOVE
        assertTrue(black.getMessages().contains("YOUR_MOVE"));
    }

    @Test
    public void testResignEndsGame() {
        FakeGame game = newGame5x5();
        FakePlayer black = game.blackPlayer;
        FakePlayer white = game.whitePlayer;

        game.resign(black);

        assertTrue(game.getState() instanceof FinishedState);

        boolean whiteWonMsg = white.getMessages().stream().anyMatch(m -> m.contains("Wygrales"));
        boolean blackLostMsg = black.getMessages().stream().anyMatch(m -> m.contains("Przegrales"));
        assertTrue(whiteWonMsg);
        assertTrue(blackLostMsg);
    }

    @Test
    public void testDeadMarkingAddsPrisonersAndClearsStone() {
        Board b = new Board(5);
        boolean[][] dead = new boolean[5][5];

        // place one white stone on board (no need to be capturable for marking test)
        b.forcePlaceStone(0, 0, StoneColor.BIALY);
        assertEquals(StoneColor.BIALY.asChar(), b.grid[0][0]);

        // mark group dead and apply
        b.toggleDeadGroup(0, 0, dead);
        b.applyDeadMarks(dead);

        assertEquals(Board.EMPTY, b.grid[0][0]);

        // white stone was dead -> prisoner for black
        assertEquals(1, b.getBlackPrisoners());
    }
}