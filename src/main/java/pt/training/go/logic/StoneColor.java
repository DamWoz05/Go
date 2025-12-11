package pt.training.go.logic;

public enum StoneColor {
    CZARNY, BIALY;

    public char asChar() {
        if (this == CZARNY) {return '\u25CF';}
        else {return '\u25CB';}
    }
}
