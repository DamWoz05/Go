package pt.training.go.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import pt.training.go.logic.Board;
import pt.training.go.logic.StoneColor;

public class GoServer {

    private static final int PORT = 1988;

    public static void main(String[] args) throws IOException {
        int size = 19; //Wielkosc Planszy

        System.out.println("Go server starting on port " + PORT + ", board size = " + size);

        try (ServerSocket listener = new ServerSocket(PORT)) {
            while (true) {
                Game game = new Game(size);
                System.out.println("Oczekiwanie na graczy..");

                //Oczekiwanie na 2 klientow i dopisywanie nim kolorow
                Game.Player black = game.new Player(listener.accept(), StoneColor.CZARNY);
                Game.Player white = game.new Player(listener.accept(), StoneColor.BIALY);

                black.setOpponent(white);
                white.setOpponent(black);

                game.setCurrentPlayer(black); //Ustawienie czarnego jako pierwszego do ruszenia

                new Thread(black).start();
                new Thread(white).start();

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
        // ZMIANA
        synchronized void makeMove(int row, int col, Player player) {
            board.move(row, col, player.color);
            currentPlayer = player.opponent;
        }

        synchronized String boardFlat() {
            return board.toFlatString();
        }

        class Player implements Runnable {
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
                    out.println("YOUR_MOVE"); // WAZNE
                    return;
                }

                int row;
                int col;
                try {

                    // Klient teraz wysyla 1..BOARD_SIZE
                    row = Integer.parseInt(parts[1]) - 1;
                    col = Integer.parseInt(parts[2]) - 1;
                } catch (NumberFormatException e) {
                    out.println("MESSAGE Wiersz i kolumna musza byc liczbami calkowitymi.");
                    out.println("YOUR_MOVE");
                    return;
                }

                synchronized (Game.this) {
                    if (!isCurrentPlayer(this)) {
                        out.println("MESSAGE To nie jest twoj ruch.");
                        return;
                    }

                    try {
                        makeMove(row, col, this);
                        out.println("MOVE_ACCEPTED " + (row + 1) + " " + (col + 1));
                        out.println("BOARD " + boardFlat()); // Aktualizacja wizualna

                        if (opponent != null && opponent.out != null) {
                            opponent.out.println("OPPONENT_MOVED " + (row + 1) + " " + (col + 1));
                            opponent.out.println("BOARD " + boardFlat());
                            //Kolej przeciwnika
                            opponent.out.println("YOUR_MOVE");
                        }
                    } catch (IllegalArgumentException ex) {
                        out.println("MESSAGE Niedozwolony ruch: " + ex.getMessage());
                        // Gracz musi sprobowac ponownie
                        out.println("YOUR_MOVE");
                    }
                }
            }
        }
    }
}
