package pt.training.go;

public class Board {

    private final int size;
    private final char[][] grid;

    public Board(int size) {
        if (size < 5) {
            throw new IllegalArgumentException("Najmniejsza mozliwa plansza to 5x5");
        }
        this.size = size;
        this.grid = new char[size][size];

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                grid[r][c] = '-';
            }
        }
    }

    public int getSize() {
        return size;
    }

    public synchronized void placeStone(int row, int col, StoneColor color) {
        if (row < 0 || row >= size || col < 0 || col >= size) {
            throw new IllegalArgumentException("Bledne miejsce kamienia podaj wartosc r oraz c w przedziale [0," + size + "]" );
        }
        grid[row][col] = color.asChar();
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

    //Konwertowanie stringa spowrotem na tabele
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

    public synchronized String toPrettyString() {
        StringBuilder sb = new StringBuilder();

        //TODO po zrobieniu logiki zmienic aby renderowanie naglowkow oraz wpisanie r c podczas ruchu zaczynalo sie od 1 a nie 0

        // naglowki dla kolumn
        sb.append("   ");
        for (int c = 0; c < size; c++) {
            if (c < 10) sb.append(' ');
            sb.append(c);
        }
        sb.append('\n');

        // wiersze
        for (int r = 0; r < size; r++) {
            if (r < 10) sb.append(' ');
            sb.append(r).append(' ');
            for (int c = 0; c < size; c++) {
                sb.append(' ').append(grid[r][c]);
            }
            sb.append('\n');
        }

        return sb.toString();
    }
}
