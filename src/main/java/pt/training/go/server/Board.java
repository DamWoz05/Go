package pt.training.go.server;

import java.util.LinkedList;
import java.util.Queue;

// TODO (FUTURE Patterns): Observer
// TODO dokonczyc dokumentacje gui dla replay
// TODO poprawic wyswietlanie listy ruchow w gui
// TODO naprawic blad gdzie polaczenie przez replay psuje odpalanie gry

/**
 * Board reprezentuje planszę gry w Go.
 * Zarządza stanem kamieni, stosuje reguły gry (m.in. KO, zakaz samobójstwa),
 * obsługuje usuwanie grup, naliczanie więźniów oraz obliczanie wyniku (scoring).
 * 
 * Nie modyfikuje wewnętrznych komentarzy użytkownika ani istniejącej logiki.
 */
public class Board {

    private final int size;
    protected final char[][] grid;
    private int blackPrisoners = 0;
    private int whitePrisoners = 0;
    protected static final char EMPTY = '+'; // Puste pola

    protected String koState = null;

    /**
     * Tworzy planszę o podanym rozmiarze.
     * Minimalny rozmiar planszy to 5 (czyli 5x5).
     *
     * @param size rozmiar planszy (liczba wierszy i kolumn)
     * @throws IllegalArgumentException gdy size < 5
     */
    public Board(int size) {
        if (size < 5) {
            throw new IllegalArgumentException("Najmniejsza mozliwa plansza to 5x5");
        }
        this.size = size;
        this.grid = new char[size][size];

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                grid[r][c] = EMPTY; // EMPTY zamiast '+'
            }
        }
    }

    /**
     * Zwraca rozmiar planszy (liczbę wierszy/kolumn).
     *
     * @return rozmiar planszy
     */
    public int getSize() {
        return size;
    }

    /**
     * Zwraca liczbę więźniów zdobytych przez czarne (liczba zbitych kamieni białych).
     *
     * @return liczba czarnych więźniów
     */
    public int getBlackPrisoners() {
        return blackPrisoners;
    }

    /**
     * Zwraca liczbę więźniów zdobytych przez białe (liczba zbitych kamieni czarnych).
     *
     * @return liczba białych więźniów
     */
    public int getWhitePrisoners() {
        return whitePrisoners;
    }

    /**
     * Wykonuje ruch na planszy zgodnie z regułami gry.
     * Sprawdza granice, zajętość pola, usuwa grupy bez oddechów,
     * sprawdza zakaz samobójstwa oraz regułę KO.
     *
     * @param row wiersz ruchu (0-indexed)
     * @param col kolumna ruchu (0-indexed)
     * @param color kolor kamienia do postawienia
     * @throws IllegalArgumentException przy nieprawidłowym ruchu
     */
    public synchronized void move(int row, int col, StoneColor color) {
        // Sprawdzenie granic
        if (row < 0 || row >= size || col < 0 || col >= size) {
            throw new IllegalArgumentException("Bledne miejsce kamienia! Podaj wartosc ROW oraz COL w przedziale [0," + size + "]");
        }

        // Sprawdzenie czy pole jest puste
        if (grid[row][col] != EMPTY) {
            throw new IllegalArgumentException("To pole jest zajete!");
        }
        // Snapshots for KO

        String snapshotGrid = toFlatString();
        int SnapshotBlackPris = blackPrisoners;
        int SnapshotWhitePris = whitePrisoners;

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
        // Aktualizacja
        if (capturedCount > 0) {
            if (color == StoneColor.CZARNY) {
                blackPrisoners += capturedCount; //czarne kill biale
            } else {
                whitePrisoners += capturedCount; //biale kill czarne
            }
        }

        // Implenetacja KO Rule
        String currentGridState = toFlatString();

        if (koState != null && currentGridState.equals(koState)) {
            // Rollback
            updateFromFlatString(snapshotGrid);
            this.blackPrisoners = SnapshotBlackPris;
            this.whitePrisoners = SnapshotWhitePris;

            throw new IllegalArgumentException("Regula KO: nie mozna powtorzyc pozycji planszy z popszedniej tury.");
        }

        this.koState = snapshotGrid;

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


    /**
     * Ustawia kamień bez sprawdzania reguł (używane do aktualizacji po stronie klienta).
     *
     * @param row wiersz
     * @param col kolumna
     * @param color kolor kamienia
     */
    public synchronized void forcePlaceStone(int row, int col, StoneColor color) {
        if (isValid(row, col)) {
            grid[row][col] = color.asChar();
        }
    }

    /**
     * Zwraca spłaszczoną reprezentację planszy (flat string).
     * Kolejne znaki odpowiadają kolejnym polom planszy.
     *
     * @return spłaszczony string reprezentujący planszę
     */
    public synchronized String toFlatString() {
        StringBuilder sb = new StringBuilder(size * size);
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                sb.append(grid[r][c]);
            }
        }
        return sb.toString();
    }

    /**
     * Zwraca spłaszczoną reprezentację planszy z oznaczonymi martwymi kamieniami ('x').
     *
     * @param deadMarks tablica oznaczająca martwe kamienie
     * @return spłaszczony string z martwymi kamieniami
     */
    public synchronized String toFlatStringWithDead(boolean[][] deadMarks) {
        StringBuilder sb = new StringBuilder(size * size);

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                char ch = grid[r][c];

                if (deadMarks != null && ch != EMPTY && deadMarks[r][c]) {
                    sb.append('x');
                } else {
                    sb.append(ch);
                }
            }
        }

        return sb.toString();
    }

    /**
     * Aktualizuje planszę na podstawie spłaszczonego stringa.
     *
     * @param flat string reprezentujący stan planszy
     * @throws IllegalArgumentException jeśli długość stringa jest nieprawidłowa
     */
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

    /**
     * Zwraca czytelną reprezentację planszy do wyświetlenia w konsoli.
     *
     * @return ładnie sformatowany string planszy
     */
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

    /**
     * Oblicza wynik gry: teren + więźniowie dla obu graczy.
     *
     * @return GameResult zawierający szczegóły wyniku
     */
    public synchronized GameResult calculateResult() {
        int blackTerritory = 0;
        int whiteTerritory = 0;
        boolean[][] visited = new boolean[size][size];

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                // Znajdujemy puste pole, ktorego jeszcze nie odwiedzilismy
                if (grid[i][j] == EMPTY && !visited[i][j]) {
                    TerritoryResult res = analyzeTerritory(i, j, visited);

                    if (res.owner == StoneColor.CZARNY) {
                        blackTerritory += res.count;
                    } else if (res.owner == StoneColor.BIALY) {
                        whiteTerritory += res.count;
                    }
                }
            }
        }

        return new GameResult(
            blackTerritory + blackPrisoners,
            whiteTerritory + whitePrisoners,
            blackTerritory,
            whiteTerritory,
            blackPrisoners,
            whitePrisoners
        );
    }

    //Flood Fill
    private TerritoryResult analyzeTerritory(int startRow, int startCol, boolean[][] visited) {
        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{startRow, startCol});
        visited[startRow][startCol] = true;

        int count = 0;
        boolean touchesBlack = false;
        boolean touchesWhite = false;

        boolean touchesTop = false;
        boolean touchesBottom = false;
        boolean touchesLeft = false;
        boolean touchesRight = false;

        int[] dr = {-1, 1, 0, 0};
        int[] dc = {0, 0, -1, 1};

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            count++;
            int r = curr[0];
            int c = curr[1];

            if (r == 0) touchesTop = true;
            if (r == size - 1) touchesBottom = true;
            if (c == 0) touchesLeft = true;
            if (c == size - 1) touchesRight = true;

            for (int i = 0; i < 4; i++) {
                int nr = r + dr[i];
                int nc = c + dc[i];

                if (isValid(nr, nc)) {
                    if (grid[nr][nc] == EMPTY && !visited[nr][nc]) {
                        visited[nr][nc] = true;
                        queue.add(new int[]{nr, nc});
                    } else if (grid[nr][nc] == StoneColor.CZARNY.asChar()) {
                        touchesBlack = true;
                    } else if (grid[nr][nc] == StoneColor.BIALY.asChar()) {
                        touchesWhite = true;
                    }
                }
            }
        }

        StoneColor owner = null;

        boolean isOpenSpace = (touchesTop && touchesBottom) || (touchesLeft && touchesRight);

        if (!isOpenSpace) {
            if (touchesBlack && !touchesWhite) {
                owner = StoneColor.CZARNY;
            } else if (touchesWhite && !touchesBlack) {
                owner = StoneColor.BIALY;
            }
        }
        
        // isOpenSpace == true, owner == null

        return new TerritoryResult(count, owner);
    }
    private static class TerritoryResult {
        int count;
        StoneColor owner;

        TerritoryResult(int count, StoneColor owner) {
            this.count = count;
            this.owner = owner;
        }
    }

    /**
     * Rezultat gry zawierający sumaryczne punkty, teren i więźniów obu graczy.
     */
    public static class GameResult {
        public int blackTotal;
        public int whiteTotal;
        public int blackTerritory;
        public int whiteTerritory;
        public int blackPrisoners;
        public int whitePrisoners;

        /**
         * Tworzy obiekt rezultatu gry.
         *
         * @param bt suma punktów czarnych (teren + więźniowie)
         * @param wt suma punktów białych (teren + więźniowie)
         * @param bTer teren czarnych
         * @param wTer teren białych
         * @param bPris więźniowie czarnych
         * @param wPris więźniowie białych
         */
        GameResult(int bt, int wt, int bTer, int wTer, int bPris, int wPris) {
            this.blackTotal = bt;
            this.whiteTotal = wt;
            this.blackTerritory = bTer;
            this.whiteTerritory = wTer;
            this.blackPrisoners = bPris;
            this.whitePrisoners = wPris;
        }

        @Override
        public String toString() {
            return "WYNIK: CZARNY " + blackTotal + " (Teren:" + blackTerritory + ", Jency:" + blackPrisoners + ") " +
                   "| BIALY " + whiteTotal + " (Teren:" + whiteTerritory + ", Jency:" + whitePrisoners + ")";
        }
    }

    // NOWE METODY: DEAD MARKING (dla SCORING)

    /**
     * Przełącza oznaczenie martwej grupy kamieni zaczynając od podanej pozycji.
     * Zmienia stan wszystkich kamieni tej grupy.
     *
     * @param row wiersz kamienia
     * @param col kolumna kamienia
     * @param dead tablica oznaczająca martwe kamienie (modyfikowana przez metodę)
     * @throws IllegalArgumentException jeśli współrzędne są nieprawidłowe lub pole jest puste
     */
    public synchronized void toggleDeadGroup(int row, int col, boolean[][] dead) {
        if (row < 0 || row >= size || col < 0 || col >= size) {
            throw new IllegalArgumentException("Bledne wspolrzedne.");
        }

        if (grid[row][col] == EMPTY) {
            throw new IllegalArgumentException("To pole jest puste.");
        }

        char stone = grid[row][col];
        boolean newValue = !dead[row][col];

        boolean[][] visited = new boolean[size][size];
        Queue<int[]> q = new LinkedList<>();
        q.add(new int[]{row, col});
        visited[row][col] = true;

        int[] dr = {-1, 1, 0, 0};
        int[] dc = {0, 0, -1, 1};

        while (!q.isEmpty()) {
            int[] cur = q.poll(); // delete
            int r = cur[0];
            int c = cur[1];

            dead[r][c] = newValue;

            for (int i = 0; i < 4; i++) {
                int nr = r + dr[i];
                int nc = c + dc[i];

                if (isValid(nr, nc) && !visited[nr][nc] && grid[nr][nc] == stone) {
                    visited[nr][nc] = true;
                    q.add(new int[]{nr, nc});
                }
            }
        }
    }

    /**
     * Zastosowuje oznaczone jako martwe kamienie, usuwa je z planszy i aktualizuje liczniki więźniów.
     *
     * @param dead tablica oznaczająca martwe kamienie (pola zostaną wyczyszczone)
     */
    public synchronized void applyDeadMarks(boolean[][] dead) {
        int blackDead = 0;
        int whiteDead = 0;

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (dead[r][c] && grid[r][c] != EMPTY) {
                    if (grid[r][c] == StoneColor.CZARNY.asChar()) {
                        blackDead++;
                    } else if (grid[r][c] == StoneColor.BIALY.asChar()) {
                        whiteDead++;
                    }
                    grid[r][c] = EMPTY;
                    dead[r][c] = false;
                }
            }
        }

        // Jesli czarne sa martwe -> jency dla bialych
        if (blackDead > 0) {
            whitePrisoners += blackDead;
        }

        // Jesli biale sa martwe -> jency dla czarnych
        if (whiteDead > 0) {
            blackPrisoners += whiteDead;
        }
    }
}