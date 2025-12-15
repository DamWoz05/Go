package pt.training.go.server;

public enum StoneColor {
    CZARNY, BIALY;

    public char asChar() {
        if (this == CZARNY) {return '\u25CB';}
        else {return '\u25CF';}
    }
}
