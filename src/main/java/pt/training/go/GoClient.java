package pt.training.go;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class GoClient {

    private BufferedReader in;
    private PrintWriter out;
    private Board board;
    private StoneColor myColor;

    public void play(String serverAddress) throws IOException {
        try (Socket socket = new Socket(serverAddress, 1988)) {

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            Scanner console = new Scanner(System.in);

            while (true) {
                String line = in.readLine();
                if (line == null) {
                    System.out.println("Polaczenie z serwerem zostalo zakonczone.");
                    break;
                }

                if (line.startsWith("WELCOME")) {
                    String colorName = line.substring("WELCOME".length()).trim();
                    myColor = StoneColor.valueOf(colorName);
                    System.out.println("Polaczono z serwerem. Twoj kolor: " + myColor);

                } else if (line.startsWith("BOARD_SIZE")) {
                    int size = Integer.parseInt(line.substring("BOARD_SIZE".length()).trim());
                    board = new Board(size);

                } else if (line.startsWith("BOARD ")) {
                    String flat = line.substring("BOARD ".length());
                    if (board != null) {
                        board.updateFromFlatString(flat);
                        System.out.println("Aktualna plansza:");
                        System.out.println(board.toPrettyString());
                    }

                } else if (line.startsWith("MESSAGE")) {
                    System.out.println(line.substring("MESSAGE".length()).trim());

                    //TODO naprawic klienta gdy poda sie zle informacje w terminalu przy ruchu
                } else if (line.startsWith("YOUR_MOVE")) {
                    System.out.println(
                            "Twoj ruch (" + myColor + "). Podaj: wiersz kolumna (np. 3 4) lub 'quit' aby wyjsc:"
                    );
                    String input = console.nextLine().trim();

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
        }
    }

    public static void main(String[] args) throws IOException {
        String serverAddress = args.length > 0 ? args[0] : "localhost";
        GoClient client = new GoClient();
        client.play(serverAddress);
    }
}
