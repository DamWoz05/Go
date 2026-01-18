package pt.training.go.server.command;

import pt.training.go.server.StoneColor;

/**
 * PlayerContext definiuje interfejs reprezentujący gracza w grze.
 * Zapewnia metody do komunikacji, zarządzania kolorem kamieni
 * i interakcji z przeciwnikiem.
 */
public interface PlayerContext {
    /**
     * Zwraca kolor kamieni gracza (BLACK lub WHITE).
     *
     * @return kolor kamieni tego gracza
     */
    StoneColor getColor();
    
    /**
     * Zwraca kontekst przeciwnika.
     *
     * @return PlayerContext reprezentujący przeciwnika
     */
    PlayerContext getOpponent();
    
    /**
     * Wysyła wiadomość do tego gracza.
     *
     * @param line wiadomość do wysłania
     */
    void send(String line);
    
    /**
     * Żąda zatrzymania połączenia gracza.
     */
    void requestStop();
}