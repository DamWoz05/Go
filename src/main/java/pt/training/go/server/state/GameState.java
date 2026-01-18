package pt.training.go.server.state;

import pt.training.go.server.command.GameContext;
import pt.training.go.server.command.PlayerContext;

/**
 * GameState definiuje interfejs dla różnych stanów gry w Go.
 * Implementacje tego interfejsu obsługują przejścia między stanami
 * i definiują zachowanie dostępne w każdym stanie.
 */
public interface GameState {
    /**
     * Obsługuje ruch gracza.
     *
     * @param game kontekst gry
     * @param player gracz wykonujący ruch
     * @param row wiersz ruchu
     * @param col kolumna ruchu
     */
    void move(GameContext game, PlayerContext player, int row, int col);
    
    /**
     * Obsługuje przejście gracza.
     *
     * @param game kontekst gry
     * @param player gracz przechodzący
     */
    void pass(GameContext game, PlayerContext player);
    
    /**
     * Obsługuje żądanie wznowienia gry.
     *
     * @param game kontekst gry
     * @param player gracz żądający wznowienia
     */
    void requestResume(GameContext game, PlayerContext player);
    
    /**
     * Obsługuje zgodę na zakończenie gry.
     *
     * @param game kontekst gry
     * @param player gracz wyrażający zgodę
     */
    void agreeEnd(GameContext game, PlayerContext player);
    
    /**
     * Obsługuje przełączenie martwości kamienia.
     *
     * @param game kontekst gry
     * @param player gracz przełączający martwość
     * @param row wiersz kamienia
     * @param col kolumna kamienia
     */
    void toggleDead(GameContext game, PlayerContext player, int row, int col);
    
    /**
     * Obsługuje rezygnację gracza.
     *
     * @param game kontekst gry
     * @param player gracz rezygnujący
     */
    void resign(GameContext game, PlayerContext player);
    
    /**
     * Obsługuje opuszczenie gry przez gracza.
     *
     * @param game kontekst gry
     * @param player gracz opuszczający grę
     */
    void quit(GameContext game, PlayerContext player);
    
    /**
     * Zwraca nazwę tego stanu gry.
     *
     * @return nazwa stanu
     */
    String getName();
}