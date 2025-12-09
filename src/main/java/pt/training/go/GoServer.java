package pt.training.go;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class GoServer {

    private static final int PORT = 1988;

    public static void main() throws IOException {
        int size = 9; //Wielkosc Planszy

        System.out.println("Go server starting on port " + PORT + ", board size = " + size);

        try (ServerSocket listener = new ServerSocket(PORT)) {
            while (true) {
                Game game = new Game(size);

                //Oczekiwanie na 2 klientow i dopisywanie nim kolorow
                Game.Player black = game.new Player(listener.accept(), StoneColor.CZARNY);
                Game.Player white = game.new Player(listener.accept(), StoneColor.BIALY);

                black.setOpponent(white);
                white.setOpponent(black);

                game.setCurrentPlayer(black); //Ustawienie czarnego jako pierwszego do ruszenia

                black.start();
                white.start();

                System.out.println("Rozpoczeto gre.");
            }
        }
    }

    private static class Game {

        private final Board board;
        private Player currentPlayer;

        Game(int size) {
            this.board = new Board(size);
        }

        synchronized void setCurrentPlayer(Player player) {
            this.currentPlayer = player;
        }

        synchronized boolean isCurrentPlayer(Player player) {
            return player == currentPlayer;
        }

        synchronized void makeMove(int row, int col, Player player) {
            board.placeStone(row, col, player.color);
            currentPlayer = player.opponent;
        }

        synchronized String boardFlat() {
            return board.toFlatString();
        }

        class Player extends Thread {
            private final Socket socket;
            private final StoneColor color;
            private Player opponent;
            private BufferedReader in;
            private PrintWriter out;

            Player(Socket socket, StoneColor color) {
                this.socket = socket;
                this.color = color;
            }

            void setOpponent(Player opponent) {
                this.opponent = opponent;
            }

            @Override
            public void run() {
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

                    while (true) {
                        String line = in.readLine();
                        if (line == null) {
                            break;
                        }

                        if (line.startsWith("MOVE")) {
                            handleMove(line);
                        } else if (line.startsWith("QUIT")) {
                            break;
                        } else {
                            out.println("MESSAGE Unknown command: " + line);
                        }
                    }

                } catch (IOException e) {
                    System.out.println("Player error: " + e.getMessage());
                } finally {
                    try {
                        socket.close();
                    } catch (IOException ignored) {}

                    if (opponent != null && opponent.out != null) {
                        opponent.out.println("OTHER_PLAYER_LEFT");
                    }
                }
            }

            private void handleMove(String line) {
                String[] parts = line.split("\\s+");
                if (parts.length < 3) {
                    out.println("MESSAGE Nieprawidlowy format komendy MOVE. Uzyj: MOVE wiersz kolumna");
                    return;
                }

                int row;
                int col;
                try {
                    row = Integer.parseInt(parts[1]);
                    col = Integer.parseInt(parts[2]);
                } catch (NumberFormatException e) {
                    out.println("MESSAGE Wiersz i kolumna musza byc liczbami calkowitymi.");
                    return;
                }

                synchronized (Game.this) {
                    if (!isCurrentPlayer(this)) {
                        out.println("MESSAGE To nie jest twoj ruch.");
                        return;
                    }

                    //TODO logika ruchow
                    try {
                        makeMove(row, col, this);
                    } catch (IllegalArgumentException ex) {
                        out.println("MESSAGE Niedozwolony ruch: " + ex.getMessage());
                        return;
                    }

                    out.println("MOVE_ACCEPTED " + row + " " + col);
                    out.println("BOARD " + boardFlat());

                    if (opponent != null && opponent.out != null) {
                        opponent.out.println("OPPONENT_MOVED " + row + " " + col);
                        opponent.out.println("BOARD " + boardFlat());
                        opponent.out.println("YOUR_MOVE");
                    }
                }
            }
        }
    }
}
