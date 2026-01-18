package pt.training.go.server.command;

/**
 * UnknownCommand reprezentuje nieznane polecenie.
 * Wysyła komunikat o nieznnym poleceniu do gracza i ponownie prosi o ruch jeśli była jego kolej.
 */
public class UnknownCommand implements Command {
    private final String raw;

    /**
     * Konstruuje UnknownCommand z surowym tekstem polecenia.
     *
     * @param raw tekst nieznnego polecenia
     */
    public UnknownCommand(String raw) {
        this.raw = raw;
    }

    /**
     * Wykonuje polecenie wysyłając komunikat o nieznanym poleceniu do gracza.
     * Jeśli było to kolej gracza, ponownie prosi go o wykonanie ruchu.
     *
     * @param game kontekst gry
     * @param player gracz, który wysłał nieznane polecenie
     */
    @Override
    public void execute(GameContext game, PlayerContext player) {
        player.send("MESSAGE Unknown command: " + raw);
        synchronized (game.lock()) {
            if (game.isCurrentPlayer(player)) {
                player.send("YOUR_MOVE");
            }
        }
    }
}