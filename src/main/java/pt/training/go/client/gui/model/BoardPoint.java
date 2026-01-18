package pt.training.go.client.gui.model;

/**
 * Punkt na planszy uzywany w GUI.
 * @param row0 wiersz 0-based
 * @param col0 kolumna 0-based
 */
public record BoardPoint(int row0, int col0) {

    /**
     * @return wiersz w formacie 1-based
     */
    public int row1() { return row0 + 1; }

    /**
     * @return kolumna w formacie 1-based
     */
    public int col1() { return col0 + 1; }
}
