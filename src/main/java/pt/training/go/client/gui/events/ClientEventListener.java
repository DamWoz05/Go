package pt.training.go.client.gui.events;

/**
 * Zdarzenia po stronie klienta.
 */
public interface ClientEventListener {

    /**
     * Serwer przydzielil kolor gracza.
     *
     * @param colorName nazwa koloru (CZARNY/BIALY)
     */
    void onWelcome(String colorName);

    /**
     * Serwer podal rozmiar planszy.
     *
     * @param size rozmiar NxN (5-19)
     */
    void onBoardSize(int size);

    /**
     * Serwer przeslal cala plansze jako spłaszczony string.
     *
     * @param flat reprezentacja planszy
     */
    void onBoard(String flat);

    /**
     * Informacja, ze to tura gracza.
     */
    void onYourMove();

    /**
     * Serwer zaakceptowal ruch gracza.
     *
     * @param row wiersz 1-based
     * @param col kolumna 1-based
     */
    void onMoveAccepted(int row, int col);

    /**
     * Serwer informuje o ruchu przeciwnika.
     *
     * @param row wiersz 1-based
     * @param col kolumna 1-based
     */
    void onOpponentMoved(int row, int col);

    /**
     * Dowolna wiadomosc tekstowa z serwera.
     *
     * @param text tresc wiadomosci
     */
    void onMessage(String text);

    /**
     * Informacja, ze drugi gracz rozlaczyl sie.
     */
    void onOtherPlayerLeft();

    /**
     * Linia protokolu nie pasuje do znanych formatow.
     *
     * @param line surowa linia z serwera
     */
    void onUnknownLine(String line);

    /**
     * Linia wyglada znajomo, ale nie dalo sie jej poprawnie sparsowac.
     *
     * @param line surowa linia z serwera
     * @param error opis bledu parsowania
     */
    void onParseError(String line, String error);
}
