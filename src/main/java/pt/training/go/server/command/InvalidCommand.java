package pt.training.go.server.command;

/**
 * InvalidCommand reprezentuje nieprawidłowe polecenie.
 * Wysyła komunikat błędu do gracza i ponownie prosi o ruch jeśli była jego kolej.
 */
public class InvalidCommand implements Command {

    private final String message;

    /**
     * Konstruuje InvalidCommand z wiadomością błędu.
     *
     * @param message wiadomość błędu do wysłania do gracza
     */
    public InvalidCommand(String message) {
        this.message = message;
    }

    /**
     * Wykonuje polecenie wysyłając komunikat błędu do gracza.
     * Jeśli było to kolej gracza, ponownie prosi go o wykonanie ruchu.
     *
     * @param game kontekst gry
     * @param player gracz, który wysłał nieprawidłowe polecenie
     */
    @Override
    public void execute(GameContext game, PlayerContext player) {
        player.send("MESSAGE " + message);
        synchronized (game.lock()) {
            if (game.isCurrentPlayer(player)) {
                player.send("YOUR_MOVE");
            }
        }
    }
}