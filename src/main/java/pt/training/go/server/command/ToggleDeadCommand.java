package pt.training.go.server.command;

/**
 * ToggleDeadCommand reprezentuje polecenie oznaczenia/odznaczenia martwego kamienia.
 * Gracz wysyła to polecenie, aby przełączyć status martwości kamienia na planszy.
 */
public class ToggleDeadCommand implements Command {

    private final int row;
    private final int col;

    /**
     * Konstruuje ToggleDeadCommand z współrzędnymi kamienia.
     *
     * @param row wiersz kamienia (0-indexed)
     * @param col kolumna kamienia (0-indexed)
     */
    public ToggleDeadCommand(int row, int col) {
        this.row = row;
        this.col = col;
    }

    /**
     * Wykonuje operację przełączenia martwości kamienia.
     * Synchronizuje dostęp do kontekstu gry i wywoła metodę toggleDeadCommand.
     *
     * @param game kontekst gry zawierający stan planszy
     * @param player gracz wykonujący operację
     */
    @Override
    public void execute(GameContext game, PlayerContext player) {
        synchronized (game.lock()) {
            game.toggleDeadCommand(row, col, player);
        }
    }
}