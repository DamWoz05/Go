package pt.training.go.server;

import pt.training.go.server.command.Command;
import pt.training.go.server.command.GameContext;
import pt.training.go.server.command.PlayerContext;
import pt.training.go.server.command.parser.CommandParser;
import pt.training.go.server.state.FinishedState;
import pt.training.go.server.state.GameState;
import pt.training.go.server.state.PlayingState;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class GoServer {

    private static final int PORT = 1988;

    public static void main(String[] args) throws IOException {
        int size = 19;

        System.out.println("Go server starting on port " + PORT + ", board size = " + size);

        try (ServerSocket listener = new ServerSocket(PORT)) {
            while (true) {
                Game game = new Game(size);
                System.out.println("Oczekiwanie na graczy..");

                Game.Player black = game.new Player(listener.accept(), StoneColor.CZARNY);
                Game.Player white = game.new Player(listener.accept(), StoneColor.BIALY);

                black.setOpponent(white);
                white.setOpponent(black);

                game.setPlayers(black, white);
                game.setCurrentPlayer(black);

                new Thread(black).start();
                new Thread(white).start();

                System.out.println("Rozpoczeto gre.");
            }
        }
    }

    private static class Game implements GameContext {

        private final Board board;
        private Player currentPlayer;
        private GameState state;

        private Player blackPlayer;
        private Player whitePlayer;

        private int consecutivePasses = 0;

        private boolean gameEnded = false;

        private boolean[][] deadMarks;

        Game(int size) {
            this.board = new Board(size);
            this.state = new PlayingState();
            this.deadMarks = new boolean[size][size];
        }

        synchronized void setPlayers(Player black, Player white) {
            this.blackPlayer = black;
            this.whitePlayer = white;
        }

        synchronized void setCurrentPlayer(Player player) {
            this.currentPlayer = player;
        }

        @Override
        public Object lock() {
            return this;
        }

        @Override
        public synchronized void setState(GameState state) {
            this.state = state;
            System.out.println("Stan gry zmieniony na: " + state.getName());

            if (state instanceof FinishedState) {
                gameEnded = true;
            }
        }

        @Override
        public synchronized void setCurrentPlayer(PlayerContext player) {
            this.currentPlayer = (Player) player;
        }

        @Override
        public Board getBoard() {
            return board;
        }

        @Override
        public synchronized boolean isCurrentPlayer(PlayerContext player) {
            return player == currentPlayer;
        }

        @Override
        public synchronized void switchTurn() {
            if (currentPlayer != null) {
                currentPlayer = currentPlayer.opponent;
                if (currentPlayer != null) {
                    currentPlayer.send("YOUR_MOVE");
                }
            }
        }

        @Override
        public synchronized void broadcast(String message) {
            if (blackPlayer != null) {
                blackPlayer.send(message);
            }
            if (whitePlayer != null) {
                whitePlayer.send(message);
            }
        }

        @Override
        public synchronized int getConsecutivePasses() {
            return consecutivePasses;
        }

        @Override
        public synchronized void resetPasses() {
            consecutivePasses = 0;
        }

        @Override
        public synchronized void incrementPasses() {
            consecutivePasses++;
        }

        @Override
        public synchronized boolean[][] getDeadMarks() {
            return deadMarks;
        }

        @Override
        public synchronized void clearDeadMarks() {
            if (deadMarks == null) return;
            for (int r = 0; r < deadMarks.length; r++) {
                for (int c = 0; c < deadMarks[r].length; c++) {
                    deadMarks[r][c] = false;
                }
            }
        }

        @Override
        public synchronized void toggleDeadCommand(int row, int col, PlayerContext playerCtx) {
            try {
                board.toggleDeadGroup(row, col, deadMarks);
                broadcast("MESSAGE [SCORING] Zmieniono status grupy (martwa/zywa).");
                broadcast("BOARD " + boardFlat());
            } catch (IllegalArgumentException e) {
                playerCtx.send("MESSAGE [SCORING] " + e.getMessage());
            }
        }

        @Override
        public synchronized void toggleDead(int row, int col, PlayerContext playerCtx) {
            try {
                board.toggleDeadGroup(row, col, deadMarks);
                broadcast("MESSAGE [SCORING] Zmieniono status grupy (martwa/zywa).");
                broadcast("BOARD " + boardFlat());
            } catch (IllegalArgumentException e) {
                playerCtx.send("MESSAGE [SCORING] " + e.getMessage());
            }
        }


        @Override
        public synchronized void applyDeadMarks() {
            board.applyDeadMarks(deadMarks);
        }

        @Override
        public synchronized void makeMove(int row, int col, PlayerContext playerCtx) {
            state.move(this, playerCtx, row, col);
        }

        @Override
        public synchronized void pass(PlayerContext playerCtx) {
            state.pass(this, playerCtx);
        }

        @Override
        public synchronized void requestResume(PlayerContext playerCtx) {
            state.requestResume(this, playerCtx);
        }

        @Override
        public synchronized void agreeEnd(PlayerContext playerCtx) {
            state.agreeEnd(this, playerCtx);
        }

        @Override
        public synchronized void resign(PlayerContext playerCtx) {
            state.resign(this, playerCtx);
        }

        @Override
        public synchronized void quit(PlayerContext playerCtx) {
            // dobrowolne QUIT -> konczymy gre "grzecznie"
            if (!gameEnded) {
                state.quit(this, playerCtx);
                gameEnded = true;
            }

            // zamykamy socket tego gracza
            Player p = (Player) playerCtx;
            p.requestStop();
        }

        private synchronized void onDisconnect(Player player) {
            if (gameEnded) {
                return;
            }

            gameEnded = true;
            setState(new FinishedState());

            Player opponent = player.opponent;
            if (opponent != null) {
                opponent.send("OTHER_PLAYER_LEFT");
            }
        }

        @Override
        public synchronized String boardFlat() {
            if (state != null && state.getName().equals("SCORING")) {
                return board.toFlatStringWithDead(deadMarks);
            }
            return board.toFlatString();
        }

        @Override
        public int getBoardSize() {
            return board.getSize();
        }

        class Player implements Runnable, PlayerContext {
            private final Socket socket;
            private final StoneColor color;
            private Player opponent;
            private BufferedReader in;
            private PrintWriter out;
            private volatile boolean running = true;

            Player(Socket socket, StoneColor color) {
                this.socket = socket;
                this.color = color;
            }

            void setOpponent(Player opponent) {
                this.opponent = opponent;
            }

            @Override
            public void run() {
                CommandParser parser = new CommandParser();

                try {
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    out = new PrintWriter(socket.getOutputStream(), true);

                    out.println("WELCOME " + color);
                    out.println("BOARD_SIZE " + board.getSize());
                    out.println("BOARD " + boardFlat());

                    if (color == StoneColor.CZARNY) {
                        out.println("MESSAGE Twoj kolor to CZARNY. Ruszasz sie jako pierwszy.");
                        out.println("YOUR_MOVE");
                    } else {
                        out.println("MESSAGE Twoj kolor to BIALY. Czekaj na pierwszy ruch przeciwnika.");
                    }

                    while (running) {
                        String line = in.readLine();
                        if (line == null) {
                            break;
                        }

                        Command command = parser.parse(line);
                        command.execute(Game.this, this);
                    }

                } catch (IOException e) {
                    System.out.println("Player error: " + e.getMessage());
                } finally {
                    try {
                        socket.close();
                    } catch (IOException ignored) {}

                    // Jesli gracz po prostu rozlaczyl sie (bez QUIT/RESIGN/itd.)
                    Game.this.onDisconnect(this);
                }
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
                if (out != null) {
                    out.println(line);
                }
            }

            @Override
            public void requestStop() {
                running = false;
                try {
                    socket.close();
                } catch (IOException ignored) {}
            }
        }
    }
}