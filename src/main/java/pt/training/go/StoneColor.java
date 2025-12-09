package pt.training.go;

public enum StoneColor {
    CZARNY, BIALY;

    public char asChar() {
        if (this == CZARNY) {return 'C';}
        else {return 'B';}
    }
}
