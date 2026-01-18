package pt.training.go.server.command;

/**
 * MoveCommand reprezentuje ruch gracza na planszy.
 * Zawiera współrzędne wiersza i kolumny docelowego pola.
 */
public class MoveCommand implements Command {
    private final int row;
    private final int col;

    /**
     * Konstruuje MoveCommand z współrzędnymi ruchu.
     *
     * @param row wiersz docelowego pola (0-indexed)
     * @param col kolumna docelowego pola (0-indexed)
     */
    public MoveCommand(int row, int col) {
        this.row = row;
        this.col = col;
    }

    /**
     * Wykonuje ruch gracza na planszy.
     * Synchronizuje dostęp do kontekstu gry i wywołuje metodę makeMove.
     *
     * @param game kontekst gry zawierający stan planszy
     * @param player gracz wykonujący ruch
     */
    @Override
    public void execute(GameContext game, PlayerContext player) {
        synchronized (game.lock()) {
            game.makeMove(row, col, player);
        }
    }
}