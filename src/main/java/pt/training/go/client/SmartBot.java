package pt.training.go.client;

import pt.training.go.server.Board;
import pt.training.go.server.StoneColor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class SmartBot {

    private BufferedReader in;
    private PrintWriter out;
    private Board board;
    private StoneColor myColor;
    private volatile boolean playing = true;
    private Random random = new Random();
    
    // Zapamiętujemy odrzucone ruchy (w formacie jaki wysyłamy serwerowi, czyli "R C")
    private Set<String> badMoves = new HashSet<>();
    private String lastMoveCoords = null; 

    private static final char EMPTY = '+';

    public void play(String serverAddress) throws IOException {
        System.out.println("[BOT] Łączę się do " + serverAddress + ":1988");
        
        try (Socket socket = new Socket(serverAddress, 1988)) {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            Thread listener = new Thread(() -> {
                try {
                    while (playing) {
                        String line = in.readLine();
                        if (line == null) {
                            playing = false;
                            break;
                        }
                        handleMessage(line);
                    }
                } catch (IOException e) {
                    if (playing) e.printStackTrace();
                }
            });
            listener.start();

            while (playing) {
                Thread.sleep(100);
            }
            
            listener.join(1000);
            
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void handleMessage(String line) {
        System.out.println("[BOT] <- " + line);
        
        if (line.startsWith("WELCOME")) {
            myColor = StoneColor.valueOf(line.substring(8).trim());
            System.out.println("[BOT] Mój kolor: " + myColor);
            
        } else if (line.startsWith("BOARD_SIZE")) {
            int size = Integer.parseInt(line.substring(11).trim());
            board = new Board(size);
            
        } else if (line.startsWith("BOARD ")) {
            badMoves.clear(); // Nowa tura = czysta lista błędów
            String flat = line.substring(6);
            board.updateFromFlatString(flat);
            
        } else if (line.startsWith("MESSAGE")) {
            String msgLower = line.toLowerCase();
            // Jeśli serwer odrzucił ruch (zajęte, błąd, zły zakres)
            if (msgLower.contains("zajete") || msgLower.contains("niedozwolony") || 
                msgLower.contains("invalid") || msgLower.contains("illegal") || 
                msgLower.contains("bledne")) {
                
                if (lastMoveCoords != null) {
                    System.out.println("[BOT] Ruch " + lastMoveCoords + " odrzucony. Ignoruję go.");
                    badMoves.add(lastMoveCoords);
                }
            }
            
        } else if (line.startsWith("YOUR_MOVE")) {
            makeMove();
            
        } else if (line.startsWith("OTHER_PLAYER_LEFT")) {
            playing = false;
        }
    }

    private void makeMove() {
        if (board == null || myColor == null) {
            sendPass();
            return;
        }

        int size = board.getSize();
        char myStone = myColor.asChar();

        // 1. OBRONA (szukanie kamieni z 1 oddechem)
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (getStone(r, c) == myStone) {
                    List<int[]> liberties = findLiberties(r, c);
                    
                    if (liberties.size() == 1) {
                        int[] lib = liberties.get(0);
                        // Konwertujemy na format serwera ("R C" gdzie R,C to 1..19)
                        String candidateMoveStr = (lib[0] + 1) + " " + (lib[1] + 1);
                        
                        if (!badMoves.contains(candidateMoveStr)) {
                            System.out.println("[BOT] OBRONA! Ratuję grupę ruchem: " + candidateMoveStr);
                            sendMove(lib[0], lib[1]);
                            return;
                        }
                    }
                }
            }
        }

        // 2. RUCH LOSOWY
        List<int[]> emptySpots = new ArrayList<>();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (getStone(r, c) == EMPTY) {
                    String moveStr = (r + 1) + " " + (c + 1);
                    if (!badMoves.contains(moveStr)) {
                        emptySpots.add(new int[]{r, c});
                    }
                }
            }
        }

        if (!emptySpots.isEmpty()) {
            int[] move = emptySpots.get(random.nextInt(emptySpots.size()));
            System.out.println("[BOT] Losowy ruch: " + (move[0]+1) + " " + (move[1]+1));
            sendMove(move[0], move[1]);
        } else {
            sendPass();
        }
    }

    /**
     * Wysyła ruch konwertując współrzędne 0-18 na 1-19.
     */
    private void sendMove(int r, int c) {
        int serverR = r + 1;
        int serverC = c + 1;
        
        String coords = serverR + " " + serverC;
        lastMoveCoords = coords; // Zapamiętujemy "5 5", a nie "4 4"
        
        out.println("MOVE " + coords);
    }
    
    private void sendPass() {
        lastMoveCoords = null;
        out.println("PASS");
    }

    private List<int[]> findLiberties(int startR, int startC) {
        int size = board.getSize();
        char color = getStone(startR, startC);
        boolean[][] visited = new boolean[size][size];
        List<int[]> liberties = new ArrayList<>();
        searchGroup(startR, startC, color, visited, liberties);
        return liberties;
    }

    private void searchGroup(int r, int c, char color, boolean[][] visited, List<int[]> liberties) {
        int size = board.getSize();
        if (r < 0 || r >= size || c < 0 || c >= size) return;
        if (visited[r][c]) return;
        
        visited[r][c] = true;
        char current = getStone(r, c);
        
        if (current == EMPTY) {
            boolean exists = false;
            for(int[] l : liberties) if(l[0]==r && l[1]==c) exists=true;
            if(!exists) liberties.add(new int[]{r, c});
            return;
        }
        
        if (current != color) return;
        
        searchGroup(r - 1, c, color, visited, liberties);
        searchGroup(r + 1, c, color, visited, liberties);
        searchGroup(r, c - 1, color, visited, liberties);
        searchGroup(r, c + 1, color, visited, liberties);
    }

    private char getStone(int r, int c) {
        int size = board.getSize();
        if (r < 0 || r >= size || c < 0 || c >= size) return 0;
        String flat = board.toFlatString();
        int index = r * size + c;
        if (index >= 0 && index < flat.length()) return flat.charAt(index);
        return EMPTY;
    }

    public static void main(String[] args) {
        String serverAddress = args.length > 0 ? args[0] : "localhost";
        SmartBot bot = new SmartBot();
        try {
            bot.play(serverAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}