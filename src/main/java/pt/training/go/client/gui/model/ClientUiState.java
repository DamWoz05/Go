package pt.training.go.client.gui.model;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import pt.training.go.server.StoneColor;

/**
 * Stan UI klienta GUI.
 *
 * Przechowuje dane potrzebne do wyswietlania gry i bindowania w JavaFX:
 * - rozmiar i zawartosc planszy,
 * - kolor gracza, faze gry oraz informacje o turze,
 * - pole wskazywane myszka (hover) i zaznaczone (selected),
 * - logi oraz historie ruchow,
 * - linie wyniku i GAME OVER.
 */
public final class ClientUiState {

    private final IntegerProperty boardSize = new SimpleIntegerProperty(0);
    private final StringProperty boardFlat = new SimpleStringProperty("");
    private final ObjectProperty<StoneColor> myColor = new SimpleObjectProperty<>(null);

    private final BooleanProperty myTurn = new SimpleBooleanProperty(false);
    private final ObjectProperty<GamePhase> phase = new SimpleObjectProperty<>(GamePhase.PLAYING);

    private final ObjectProperty<BoardPoint> hover = new SimpleObjectProperty<>(null);
    private final ObjectProperty<BoardPoint> selected = new SimpleObjectProperty<>(null);

    private final StringProperty lastScoreLine = new SimpleStringProperty(null);

    private final ObservableList<String> logs = FXCollections.observableArrayList();
    private final ObservableList<String> moves = FXCollections.observableArrayList();

    private final StringProperty lastGameOverLine = new SimpleStringProperty(null);

    /**
     * @return property z linia GAME OVER, ustawiana po zakonczeniu gry
     */
    public StringProperty lastGameOverLineProperty() { return lastGameOverLine; }

    /**
     * Ustawia linie GAME OVER.
     *
     * @param s tresc linii GAME OVER
     */
    public void setLastGameOverLine(String s) { lastGameOverLine.set(s); }

    /**
     * @return property z rozmiarem planszy NxN
     */
    public IntegerProperty boardSizeProperty() { return boardSize; }

    /**
     * @return property ze splaszczona plansza
     */
    public StringProperty boardFlatProperty() { return boardFlat; }

    /**
     * @return property informujaca, czy to tura gracza
     */
    public BooleanProperty myTurnProperty() { return myTurn; }

    /**
     * @return property z faza gry
     */
    public ObjectProperty<GamePhase> phaseProperty() { return phase; }

    /**
     * @return property z polem wskazywanym myszka
     */
    public ObjectProperty<BoardPoint> hoverProperty() { return hover; }

    /**
     * @return property z polem zaznaczonym kliknieciem
     */
    public ObjectProperty<BoardPoint> selectedProperty() { return selected; }

    /**
     * @return lista logow/informacji do wyswietlenia w GUI
     */
    public ObservableList<String> logs() { return logs; }

    /**
     * @return lista ruchow (historia)
     */
    public ObservableList<String> moves() { return moves; }

    /**
     * Ustawia rozmiar planszy.
     *
     * @param n rozmiar NxN
     */
    public void setBoardSize(int n) { boardSize.set(n); }

    /**
     * Ustawia splaszczona plansze.
     *
     * @param flat reprezentacja planszy
     */
    public void setBoardFlat(String flat) { boardFlat.set(flat); }

    /**
     * @return kolor gracza nadany przez serwer
     */
    public StoneColor getMyColor() { return myColor.get(); }

    /**
     * @param c kolor gracza nadany przez serwer
     */
    public void setMyColor(StoneColor c) { myColor.set(c); }

    /**
     * @return true jesli to tura gracza
     */
    public boolean isMyTurn() { return myTurn.get(); }

    /**
     * @param v ustawienie informacji o turze gracza
     */
    public void setMyTurn(boolean v) { myTurn.set(v); }

    /**
     * @return aktualna faza gry
     */
    public GamePhase getPhase() { return phase.get(); }

    /**
     * @param p nowa faza gry
     */
    public void setPhase(GamePhase p) { phase.set(p); }

    /**
     * @param p pole wskazywane myszka
     */
    public void setHover(BoardPoint p) { hover.set(p); }

    /**
     * @param p pole zaznaczone kliknieciem
     */
    public void setSelected(BoardPoint p) { selected.set(p); }

    /**
     * @return linia z wynikiem
     */
    public String getLastScoreLine() { return lastScoreLine.get(); }

    /**
     * @param s linia z wynikiem
     */
    public void setLastScoreLine(String s) { lastScoreLine.set(s); }
}
