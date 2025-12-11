package pt.training.go.logic;

public class Board {

    private final int size;
    private final char[][] grid;
    // 
    private int blackPrisoners = 0;
    private int whitePrisoners = 0;
    // Puste pola
    private static final char EMPTY = '+';

    public Board(int size) {
        if (size < 5) {
            throw new IllegalArgumentException("Najmniejsza mozliwa plansza to 5x5");
        }
        this.size = size;
        this.grid = new char[size][size];

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                grid[r][c] = EMPTY; // EMPTY zamiast '-'
            }
        }
    }

    public int getSize() {
        return size;
    }

    public int getBlackPrisoners() {
        return blackPrisoners;
    }

    public int getWhitePrisoners() {
        return whitePrisoners;
    }

    // MAIN GAME LOGIC 
    public synchronized void move(int row, int col, StoneColor color) {
        // Sprawdzenie granic 
        if (row < 0 || row >= size || col < 0 || col >= size) {
            throw new IllegalArgumentException("Bledne miejsce kamienia! Podaj wartosc ROW oraz COL w przedziale [0," + size + "]");
        }

        // Sprawdzenie czy pole jest puste
        if (grid[row][col] != EMPTY) {
            throw new IllegalArgumentException("To pole jest zajete!");
        }

        char myStone = color.asChar();
        char oppStone = (color == StoneColor.CZARNY) ? StoneColor.BIALY.asChar() : StoneColor.CZARNY.asChar();

        // Stawiamy kamien aby sprawdzic wplyw na oddechy 
        grid[row][col] = myStone;

        // Oddechy
        int capturedCount = 0;
        int[] dr = {-1, 1, 0, 0}; // Przesuniecia wierszy (gora, dol)
        int[] dc = {0, 0, -1, 1}; // Przesuniecia column (lewo, prawo) 

        for (int i = 0; i < 4; i++) {
            int nr = row + dr[i];
            int nc = col + dc[i];
            // Jesli sasiad - kamien przeciwnika
            if (isValid(nr, nc) && grid[nr][nc] == oppStone) {
                // Jesli 0 oddechow - usuwamy
                if (!hasLiberties(nr, nc)) {
                    capturedCount += removeGroup(nr, nc);
                }
            }
        }

        // Zakaz samobojstwa (Suicide Rule)
        if (capturedCount == 0 && !hasLiberties(row, col)) {
            // Cofamy ruch (Rollback)
            grid[row][col] = EMPTY;
            throw new IllegalArgumentException("Ruch samobojczy jest zabroniony! (brak oddechów)");
        }
        // Doszlismy tutaj > ruch jest poprawny > kamien zostaje

        if (capturedCount > 0) {
            if (color == StoneColor.CZARNY) {
                blackPrisoners += capturedCount; //czarne kill biale
            } else {
                whitePrisoners += capturedCount; //biale kill czarne
            }
        }
    }

    // ALGORYTMY POMOCNICZE 

    // Sprawdzenie czy jest chociaż 1 oddech obok 
    private boolean hasLiberties(int r, int c) {
        boolean[][] visited = new boolean[size][size];
        return checkGroupLiberties(r, c, grid[r][c], visited);
    }

    // Rekursywnie szukamy oddechy
    private boolean checkGroupLiberties(int r, int c, char color, boolean[][] visited) {
        visited[r][c] = true;
        int[] dr = {-1, 1, 0, 0};
        int[] dc = {0, 0, -1, 1};

        for (int i = 0; i < 4; i++) {
            int nr = r + dr[i];
            int nc = c + dc[i];

            if (isValid(nr, nc)) {
                // Znalezenie pustego miejsca
                if (grid[nr][nc] == EMPTY) {
                    return true;
                }
                // Sasiad tego samego koloru i my tam nie byliśmy > szukamy dalej
                if (grid[nr][nc] == color && !visited[nr][nc]) {
                    if (checkGroupLiberties(nr, nc, color, visited)) {
                        return true;
                    }
                }
            }
        }
        return false; // Nie zanleziono zadnego oddechu dla calej grupy
    }

    // Usuwa grupe kamieni bez oddechow i zwraca liczbe usunietych  
    private int removeGroup(int r, int c) {
        char colorToRemove = grid[r][c];
        grid[r][c] = EMPTY;
        int count = 1;

        int[] dr = {-1, 1, 0, 0};
        int[] dc = {0, 0, -1, 1};

        for (int i = 0; i < 4; i++) {
            int nr = r + dr[i];
            int nc = c + dc[i];

            if (isValid(nr, nc) && grid[nr][nc] == colorToRemove) {
                count += removeGroup(nr, nc);
            }
        }
        return count;
    }

    // Sprawdza, czy wspolrzedne mieszcza sie w planszy
    private boolean isValid(int r, int c) {
        return r >= 0 && r < size && c >= 0 && c < size;
    }

    // METODY SYNCHRONIZACJI I OPTYMALIZACJI
    

    // Ustawia kamien bez sprawdzania regul (dla aktualizacji planszy u klienta)
    public synchronized void forcePlaceStone(int row, int col, StoneColor color) {
        if (isValid(row, col)) {
            grid[row][col] = color.asChar();
        }
    }

    //Konwertowanie tabeli na string dla lepszej optymalizacji
    public synchronized String toFlatString() {
        StringBuilder sb = new StringBuilder(size * size);
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                sb.append(grid[r][c]);
            }
        }
        return sb.toString();
    }

    //Konwertowanie stringa z powrotem na tabele
    public synchronized void updateFromFlatString(String flat) {
        if (flat.length() != size * size) {
            throw new IllegalArgumentException("String z informacjami o planszy ma zla dlugosc");
        }
        int index = 0;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                grid[r][c] = flat.charAt(index++);
            }
        }
    }

    // WIZUALIZACJA 
    public synchronized String toPrettyString() {
        StringBuilder sb = new StringBuilder();

        // naglowki dla kolumn 
        sb.append("   ");
        for (int c = 0; c < size; c++) {
            int displayNum = c + 1; // 0>1
            if (displayNum < 10) {
                sb.append("  ").append(displayNum); // 2 spacje dla 1-cyfrowych
            } else {
                sb.append(" ").append(displayNum); // 1 spacja dla 2-cyfrowych
            }
        }
        sb.append('\n');

        // wiersze
        for (int r = 0; r < size; r++) {
            int displayNum = r + 1;

            // Numer wiersza z wyrownaniem
            if (displayNum < 10) sb.append(" ");
            sb.append(displayNum).append(" ");
            // Rysowanie kamieni

            for (int c = 0; c < size; c++) {
                // Dopasowanie do naglowkow column
                sb.append("  ").append(grid[r][c]);
            }
            sb.append('\n');
        }

        return sb.toString();
    }
}