package pt.training.go.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import pt.training.go.server.Board;
import pt.training.go.server.StoneColor;


import java.util.Scanner;

public class GoClient {

    private BufferedReader in;
    private PrintWriter out;
    private Board board;
    private StoneColor myColor;

    public void play(String serverAddress) throws IOException {
        System.out.println("Laczenie z serwerem " + serverAddress + " na porcie 1988...");
        // Naprawiono scanner
        try (Socket socket = new Socket(serverAddress, 1988);
        Scanner console = new Scanner(System.in)) {

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            while (true) {
                String line = in.readLine();
                if (line == null) {
                    System.out.println("Polaczenie z serwerem zostalo zakonczone.");
                    break;
                }

                if (line.startsWith("WELCOME")) {
                    String colorName = line.substring("WELCOME".length()).trim();
                    try {
                        myColor = StoneColor.valueOf(colorName);
                        System.out.println("Polaczono z serwerem! Twoj kolor: " + myColor);
                    } catch (IllegalArgumentException e) {
                        System.out.println("Blad: Nieznany kolor od serwera: " + colorName);
                    }
                } else if (line.startsWith("BOARD_SIZE")) {
                    int size = Integer.parseInt(line.substring("BOARD_SIZE".length()).trim());
                    board = new Board(size);
                    System.out.println("Utworzono plansze o rozmiarze: " + size + "x" + size);

                } else if (line.startsWith("BOARD ")) {
                    String flat = line.substring("BOARD ".length());
                    if (board != null) {
                        board.updateFromFlatString(flat);
                        System.out.println("\nAktualna plansza:");
                        System.out.println(board.toPrettyString());
                    }

                } else if (line.startsWith("MESSAGE")) {
                    System.out.println(line.substring("MESSAGE".length()).trim());

                } else if (line.startsWith("YOUR_MOVE")) {

                    String input;
                    while (true) {
                        System.out.println(
                                "TWOJ RUCH (" + myColor + "). Podaj: wiersz kolumna (np. 3 4) lub 'quit' aby wyjsc:"
                        );
                        if (console.hasNextLine()) {
                            input = console.nextLine().trim();
                            if (input.isEmpty()) {
                                continue; 
                            } 

                            break;
                        } else {
                            System.out.println("EOF: Koncze gre..");
                            input = "quit";
                            break;
                        }
                    }

                        if (input.equalsIgnoreCase("quit")) {
                            out.println("QUIT");
                            break;
                        } else {
                            out.println("MOVE " + input);
                        }

                } else if (line.startsWith("MOVE_ACCEPTED")) {
                    String coords = line.substring("MOVE_ACCEPTED".length()).trim();
                    System.out.println("Ruch zaakceptowany: " + coords);

                } else if (line.startsWith("OPPONENT_MOVED")) {
                    String coords = line.substring("OPPONENT_MOVED".length()).trim();
                    System.out.println("Przeciwnik wykonal ruch: " + coords);

                } else if (line.startsWith("OTHER_PLAYER_LEFT")) {
                    System.out.println("Przeciwnik rozlaczyl sie. Koniec gry.");
                    break;

                } else {
                    System.out.println("Nieznana wiadomosc z serwera: " + line);
                }
            
            } 
        } catch (IOException e) {
            System.out.println("Blad polaczenia: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        String serverAddress = args.length > 0 ? args[0] : "localhost";
        GoClient client = new GoClient();
        try {
            client.play(serverAddress);
        } catch (IOException e) {
            System.out.println("Wystapil blad: " + e.getMessage());
        }
    }
}
