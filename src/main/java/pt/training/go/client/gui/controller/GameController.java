package pt.training.go.client.gui.controller;

import pt.training.go.client.gui.events.ClientEventListener;
import pt.training.go.client.gui.model.BoardPoint;
import pt.training.go.client.gui.model.ClientUiState;
import pt.training.go.client.gui.model.GamePhase;
import pt.training.go.client.gui.net.ClientConnection;
import pt.training.go.server.StoneColor;

/**
 * Kontroler klienta GUI.
 *
 * Odbiera zdarzenia z parsera (wiadomosci z serwera), aktualizuje stan UI
 * oraz wysyla komendy do serwera (MOVE/PASS/RESIGN/TOGGLE_DEAD/AGREE_END/REQUEST_RESUME).
 *
 * Zasady:
 * - GUI uzywa wspolrzednych 0-based (BoardPoint), serwer 1-based (row1/col1).
 * - W PLAYING wysylamy MOVE/PASS tylko gdy to nasza tura.
 * - W SCORING wysylamy TOGGLE_DEAD niezaleznie od tury (serwer prowadzi scoring).
 */
public final class GameController implements ClientEventListener {

    private final ClientUiState state;
    private ClientConnection connection;

    /**
     * Tworzy kontroler oparty o podany stan UI.
     *
     * @param state stan UI, ktory bedzie aktualizowany
     */
    public GameController(ClientUiState state) {
        this.state = state;
    }

    /**
     * Zwraca model stanu UI kontrolowany przez ten kontroler.
     *
     * @return stan UI
     */
    public ClientUiState state() {
        return state;
    }

    /**
     * Podpina polaczenie sieciowe uzywane do wysylania komend.
     *
     * @param connection aktywne polaczenie do serwera
     */
    public void attachConnection(ClientConnection connection) {
        this.connection = connection;
    }

    /**
     * Obsluga wiadomosci powitalnej, ustawia kolor gracza.
     *
     * @param colorName nazwa koloru z serwera (CZARNY/BIALY)
     */
    @Override
    public void onWelcome(String colorName) {
        if ("REPLAY".equalsIgnoreCase(colorName)) {
            state.setMyColor(null);
            state.setPhase(GamePhase.REPLAY);
            state.setMyTurn(false);
            state.logs().add("Tryb Replay: Wstecz/Dalej (PREV/NEXT)");
            return;
        }
        try {
            StoneColor c = StoneColor.valueOf(colorName);
            state.setMyColor(c);
            state.logs().add("Twoj kolor: " + c);
        } catch (IllegalArgumentException e) {
            state.logs().add("Blad: nieznany kolor z serwera: " + colorName);
        }
    }

    /**
     * Ustawia rozmiar planszy.
     *
     * @param size rozmiar NxN (5-19)
     */
    //TODO  ustalanie rozmiaru planszy przy odpalaniu programu
    @Override
    public void onBoardSize(int size) {
        state.setBoardSize(size);
        state.logs().add("Plansza: " + size + "x" + size);
    }

    /**
     * Aktualizuje zawartosc planszy na podstawie "flat string" od serwera.
     *
     * @param flat spłaszczona reprezentacja planszy
     */
    @Override
    public void onBoard(String flat) {
        state.setBoardFlat(flat);
    }

    /**
     * Informacja, ze to nasza tura.
     * Ustawia faze PLAYING i odblokowuje akcje ruchu.
     */
    @Override
    public void onYourMove() {
        state.setPhase(GamePhase.PLAYING);
        state.setMyTurn(true);
        state.logs().add("Twoj ruch.");
    }

    /**
     * Serwer zaakceptowal nasz ruch.
     *
     * @param row wiersz 1-based
     * @param col kolumna 1-based
     */
    @Override
    public void onMoveAccepted(int row, int col) {
        state.moves().add("TY: " + row + " " + col);
    }

    /**
     * Ruch przeciwnika.
     *
     * @param row wiersz 1-based
     * @param col kolumna 1-based
     */
    @Override
    public void onOpponentMoved(int row, int col) {
        state.moves().add("PRZECIWNIK: " + row + " " + col);
    }

    /**
     * Dowolna wiadomosc tekstowa z serwera.
     *
     * @param text tresc wiadomosci
     */
    @Override
    public void onMessage(String text) {
        state.logs().add(text);

        if (text.startsWith("WYNIK:")) {
            state.setLastScoreLine(text);
        }

        if (looksLikeScoringMessage(text)) {
            state.setPhase(GamePhase.SCORING);
        }

        if (text.startsWith("[GAME OVER]")) {
            state.setLastGameOverLine(text);
            state.setPhase(GamePhase.FINISHED);
            state.setMyTurn(false);
        }

        String up = text.toUpperCase();

        if (up.startsWith("WZNOWILES GRE")
                || up.startsWith("PRZECIWNIK WZNOWIL GRE")
                || up.contains("ZAZADAL WZNOWIENIA GRY")) {
            state.setPhase(GamePhase.PLAYING);
            state.setMyTurn(false);
        }
    }

    /**
     * Przeciwnik rozlaczyl sie z gry.
     */
    @Override
    public void onOtherPlayerLeft() {
        state.logs().add("Przeciwnik rozlaczyl sie.");
        state.setPhase(GamePhase.FINISHED);
        state.setMyTurn(false);
    }

    /**
     * Linia protokolu nie pasuje do znanych formatow.
     *
     * @param line surowa linia z serwera
     */
    @Override
    public void onUnknownLine(String line) {
        state.logs().add("Nieznana linia: " + line);
    }

    /**
     * Linia wyglada znajomo, ale nie dalo sie jej sparsowac.
     *
     * @param line surowa linia z serwera
     * @param error opis bledu parsowania
     */
    @Override
    public void onParseError(String line, String error) {
        state.logs().add("Blad parsowania: " + error + " | linia: " + line);
    }

    /**
     * Wykrywania wejscia w fazie SCORING na podstawie tresci wiadomosci.
     *
     * @param text tresc wiadomosci z serwera
     * @return true jesli wiadomosc sugeruje scoring
     */
    private boolean looksLikeScoringMessage(String text) {
        String t = text.toUpperCase();
        return t.contains("SCORING") || t.contains("[SCORING]") || t.contains("OZNACZ");
    }

    /**
     * Wysyla ruch do serwera (MOVE) dla kliknietego pola.
     * Dziala tylko w fazie PLAYING i tylko w naszej turze.
     *
     * @param p punkt planszy (0-based)
     */
    public void sendMove(BoardPoint p) {
        if (!canSend()) return;
        if (state.getPhase() != GamePhase.PLAYING) return;
        if (!state.isMyTurn()) return;

        state.setMyTurn(false);
        connection.sendLine("MOVE " + p.row1() + " " + p.col1());
    }

    /**
     * Wysyla PASS do serwera.
     * Dziala tylko w fazie PLAYING i tylko w naszej turze.
     */
    public void sendPass() {
        if (!canSend()) return;

        if (state.getPhase() == GamePhase.PLAYING) {
            if (!state.isMyTurn()) return;
            state.setMyTurn(false);
            connection.sendLine("PASS");
            return;
        }
    }

    /**
     * Wysyla RESIGN do serwera i poddaje gre.
     */
    public void sendResign() {
        if (!canSend()) return;
        if (state.getPhase() == GamePhase.FINISHED) return;
        connection.sendLine("RESIGN");
    }

    /**
     * Wysyla TOGGLE_DEAD w fazie SCORING, aby oznaczyc/odznaczyc martwa grupe.
     *
     * @param p punkt planszy (0-based)
     */
    public void sendToggleDead(BoardPoint p) {
        if (!canSend()) return;
        if (state.getPhase() != GamePhase.SCORING) return;
        connection.sendLine("TOGGLE_DEAD " + p.row1() + " " + p.col1());
    }

    /**
     * Wysyla AGREE_END, czyli akceptacje wyniku w fazie SCORING.
     * Gra konczy sie dopiero gdy obaj gracze wysla AGREE_END.
     */
    public void sendAgreeEnd() {
        if (!canSend()) return;
        connection.sendLine("AGREE_END");
    }

    /**
     * Wysyla REQUEST_RESUME, czyli prosbe o wznowienie gry po scoringu.
     * Po stronie klienta przechodzimy do PLAYING i czekamy na YOUR_MOVE.
     */
    public void sendRequestResume() {
        if (!canSend()) return;
        connection.sendLine("REQUEST_RESUME");
        state.setPhase(GamePhase.PLAYING);
    }

    public void sendReplayNext() {
        if (state.getPhase() != GamePhase.REPLAY) return;
        connection.sendLine("NEXT");
    }

    public void sendReplayPrev() {
        if (state.getPhase() != GamePhase.REPLAY) return;
        connection.sendLine("PREV");
    }

    /**
     * Sprawdza czy mozna wysylac komendy do serwera.
     *
     * @return true gdy polaczenie istnieje i jest aktywne
     */
    private boolean canSend() {
        return connection != null && connection.isRunning();
    }
}
