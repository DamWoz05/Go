package pt.training.go.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

public class SmartBot {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int PORT = 1988;
    private static final int SIZE = 19;

    private static final char SYMBOL_BLACK = '\u25CB';
    private static final char SYMBOL_WHITE = '\u25CF';

    private char myColorChar;
    private char oppColorChar;
    private String lastMove = "";

    public static void main(String[] args) {
        new SmartBot().start();
    }

    public void start() {
        try (Socket socket = new Socket(SERVER_ADDRESS, PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            char[][] board = new char[SIZE][SIZE];
            String line;

            while ((line = in.readLine()) != null) {

                if (line.startsWith("WELCOME")) {
                    String[] parts = line.split(" ");
                    String color = parts.length > 1 ? parts[1] : "";
                    if (color.equals("CZARNY") || color.equals("BLACK")) {
                        myColorChar = SYMBOL_BLACK;
                        oppColorChar = SYMBOL_WHITE;
                    } else {
                        myColorChar = SYMBOL_WHITE;
                        oppColorChar = SYMBOL_BLACK;
                    }
                }

                else if (line.startsWith("BOARD")) {
                    parseBoard(line.substring(6), board);
                }

                else if (line.startsWith("YOUR_MOVE")) {
                    String move = calculateMove(board);
                    if (move.equals(lastMove)) {
                        move = makeRandomMove(board);
                    }
                    out.println(move);
                    lastMove = move;
                }

                else if (line.startsWith("MESSAGE [GAME OVER]")) {
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String calculateMove(char[][] board) {

        String defense = findCriticalMove(board, myColorChar);
        if (defense != null) return defense;

        String attack = findCriticalMove(board, oppColorChar);
        if (attack != null) return attack;

        return makeRandomMove(board);
    }

    private String findCriticalMove(char[][] board, char targetColor) {

        Set<String> visited = new HashSet<>();

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {

                String key = r + "," + c;
                if (board[r][c] == targetColor && !visited.contains(key)) {

                    Set<String> group = new HashSet<>();
                    Set<String> liberties = new HashSet<>();
                    getGroupAndLiberties(board, r, c, targetColor, group, liberties);
                    visited.addAll(group);

                    if (liberties.size() == 1) {
                        String lib = liberties.iterator().next();
                        String[] p = lib.split(",");
                        int lr = Integer.parseInt(p[0]);
                        int lc = Integer.parseInt(p[1]);

                        if (isValidMove(board, lr, lc)) {
                            return "MOVE " + lr + " " + lc;
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean isValidMove(char[][] board, int r, int c) {

        if (r < 0 || r >= SIZE || c < 0 || c >= SIZE) return false;
        if (board[r][c] == myColorChar || board[r][c] == oppColorChar) return false;

        char[][] copy = copyBoard(board);
        copy[r][c] = myColorChar;

        boolean captures = capturesOpponent(copy, r, c);

        Set<String> group = new HashSet<>();
        Set<String> liberties = new HashSet<>();
        getGroupAndLiberties(copy, r, c, myColorChar, group, liberties);

        return !liberties.isEmpty() || captures;
    }

    private boolean capturesOpponent(char[][] board, int r, int c) {

        int[][] dirs = {{0,1},{0,-1},{1,0},{-1,0}};

        for (int[] d : dirs) {
            int nr = r + d[0];
            int nc = c + d[1];

            if (nr < 0 || nr >= SIZE || nc < 0 || nc >= SIZE) continue;

            if (board[nr][nc] == oppColorChar) {
                Set<String> g = new HashSet<>();
                Set<String> l = new HashSet<>();
                getGroupAndLiberties(board, nr, nc, oppColorChar, g, l);
                if (l.isEmpty()) return true;
            }
        }
        return false;
    }

    private void getGroupAndLiberties(
            char[][] board, int r, int c, char color,
            Set<String> group, Set<String> liberties) {

        Queue<int[]> q = new ArrayDeque<>();
        q.add(new int[]{r, c});
        group.add(r + "," + c);

        int[][] dirs = {{0,1},{0,-1},{1,0},{-1,0}};

        while (!q.isEmpty()) {
            int[] cur = q.poll();

            for (int[] d : dirs) {
                int nr = cur[0] + d[0];
                int nc = cur[1] + d[1];

                if (nr < 0 || nr >= SIZE || nc < 0 || nc >= SIZE) continue;

                String key = nr + "," + nc;
                char cell = board[nr][nc];

                if (cell == color && !group.contains(key)) {
                    group.add(key);
                    q.add(new int[]{nr, nc});
                }
                else if (cell != myColorChar && cell != oppColorChar) {
                    liberties.add(key);
                }
            }
        }
    }

    private String makeRandomMove(char[][] board) {

        List<int[]> moves = new ArrayList<>();

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (isValidMove(board, r, c)) {
                    moves.add(new int[]{r, c});
                }
            }
        }

        if (moves.isEmpty()) return "PASS";

        Collections.shuffle(moves);
        int[] m = moves.get(0);
        return "MOVE " + m[0] + " " + m[1];
    }

    private char[][] copyBoard(char[][] board) {
        char[][] copy = new char[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) {
            System.arraycopy(board[i], 0, copy[i], 0, SIZE);
        }
        return copy;
    }

    private void parseBoard(String flat, char[][] board) {
        flat = flat.trim();
        for (int i = 0; i < flat.length() && i < SIZE * SIZE; i++) {
            int r = i / SIZE;
            int c = i % SIZE;
            char sym = flat.charAt(i);
            if (sym == 'B') sym = SYMBOL_BLACK;
            if (sym == 'W') sym = SYMBOL_WHITE;
            board[r][c] = sym;
        }
    }
}
