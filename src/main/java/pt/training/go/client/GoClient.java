package pt.training.go.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import pt.training.go.server.Board;
import pt.training.go.server.StoneColor;

import java.util.Scanner;

/**
 * GoClient jest aplikacją kliencką do gry w Go.
 * Łączy się z serwerem GoServer przez gniazdo TCP i obsługuje komunikację
 * dotyczącą ruchów, aktualizacji planszy i zarządzania stanem gry.
 */
public class GoClient {

    private BufferedReader in;
    private PrintWriter out;
    private Board board;
    private StoneColor myColor;
    private volatile boolean playing = true;

    /**
     * Łączy się z serwerem Go i rozpoczyna grę.
     * Nawiązuje połączenie gniazda, zarządza strumieniami wejścia/wyjścia
     * i obsługuje zarówno wiadomości serwera jak i wejście użytkownika równocześnie.
     *
     * @param serverAddress adres serwera Go, z którym się łączyć
     * @throws IOException jeśli wystąpi błąd I/O podczas połączenia lub komunikacji
     */
    public void play(String serverAddress) throws IOException {
        System.out.println("Laczenie z serwerem " + serverAddress + " na porcie 1988...");
        
        try (Socket socket = new Socket(serverAddress, 1988);
        Scanner console = new Scanner(System.in)) {

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            Thread listener = new Thread(() -> {
                try {
                    while (playing) {
                        String line = in.readLine();
                        if (line == null) {
                            System.out.println("Polaczenie z serwerem zostalo zakonczone.");
                            playing = false;
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
                            System.out.println(
                                    "TWOJ RUCH (" + myColor + "). Podaj: wiersz kolumna (np. 3 4) lub 'quit' aby wyjsc:"
                            );

                        } else if (line.startsWith("MOVE_ACCEPTED")) {
                            String coords = line.substring("MOVE_ACCEPTED".length()).trim();
                            System.out.println("Ruch zaakceptowany: " + coords);

                        } else if (line.startsWith("OPPONENT_MOVED")) {
                            String coords = line.substring("OPPONENT_MOVED".length()).trim();
                            System.out.println("Przeciwnik wykonal ruch: " + coords);

                        } else if (line.startsWith("OTHER_PLAYER_LEFT")) {
                            System.out.println("Przeciwnik rozlaczyl sie. Koniec gry.");
                            playing = false;
                            break;

                        } else {
                            System.out.println("Nieznana wiadomosc z serwera: " + line);
                        }
                    }
                } catch (IOException e) {
                    if (playing) System.out.println("Blad polaczenia: " + e.getMessage());
                }
            });
            listener.start();

            while (playing) {
                if (console.hasNextLine()) {
                    String input = console.nextLine().trim();
                    if (input.isEmpty()) {
                        continue; 
                    } 

                    if (input.equalsIgnoreCase("quit")) {
                        out.println("QUIT");
                        playing = false;
                        break;
                    } else if (input.equalsIgnoreCase("pass")) {
                        out.println("PASS");
                    } else if (input.equalsIgnoreCase("resign")) {
                        out.println("RESIGN");
                    } else if (input.equalsIgnoreCase("resume")) {
                        out.println("REQUEST_RESUME");
                    } else if (input.equalsIgnoreCase("agree")) {
                        out.println("AGREE_END");
                    } else if (input.toLowerCase().startsWith("dead ")) {
                        out.println("TOGGLE_DEAD " + input.substring(5).trim());
                    } else {
                        String[] parts = input.split("\\s+");
                        if (parts.length == 2) {
                            try {
                                Integer.parseInt(parts[0]);
                                Integer.parseInt(parts[1]);
                                out.println("MOVE " + input);
                            } catch (NumberFormatException e) {
                                System.out.println("Nieprawidlowy ruch. Uzyj formatu: wiersz kolumna (np. 3 4)");
                            }
                        } else {
                            System.out.println("Nieprawidlowy ruch. Uzyj formatu: wiersz kolumna (np. 3 4)");
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Blad polaczenia: " + e.getMessage());
        }
    }

    /**
     * Główny punkt wejścia aplikacji klienta Go.
     * Akceptuje opcjonalny adres serwera jako argument wiersza poleceń.
     *
     * @param args argumenty wiersza poleceń, gdzie args[0] to adres serwera
     *             (domyślnie "localhost" jeśli nie jest podany)
     * @throws IOException jeśli wystąpi błąd I/O
     */
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

