package pt.training.go.server.command;

/**
 * Command jest interfejsem reprezentującym polecenie do wykonania w grze.
 * Wszystkie polecenia gracza implementują ten interfejs i definiują swoją logikę
 * w metodzie execute.
 */
public interface Command {
    /**
     * Wykonuje polecenie w kontekście gry.
     *
     * @param game kontekst gry zawierający stan rozgrywki
     * @param player kontekst gracza wykonującego polecenie
     */
    void execute(GameContext game, PlayerContext player);
}