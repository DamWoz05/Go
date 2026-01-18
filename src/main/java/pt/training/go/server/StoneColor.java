package pt.training.go.server;

/**
 * Reprezentuje kolory kamieni używane w grze Go.
 * Zawiera wartości dla obu graczy: CZARNY i BIALY.
 */
public enum StoneColor {
    CZARNY, BIALY;

    /**
     * Zwraca znak Unicode używany do graficznej reprezentacji kamienia danego koloru.
     *
     * @return znak reprezentujący kamień dla tego koloru
     */
    public char asChar() {
        if (this == CZARNY) {return '\u25CB';}
        else {return '\u25CF';}
    }
}
