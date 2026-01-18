package pt.training.go.server.command;

/**
 * ResignCommand reprezentuje polecenie rezygnacji gracza.
 * Gracz wysyła to polecenie, aby się poddać i zakończyć grę.
 */
public class ResignCommand implements Command {

    /**
     * Wykonuje operację rezygnacji gracza.
     * Synchronizuje dostęp do kontekstu gry i wywoła metodę resign.
     *
     * @param game kontekst gry zawierający stan rozgrywki
     * @param player gracz rezygnujący z gry
     */
    @Override
    public void execute(GameContext game, PlayerContext player) {
        synchronized (game.lock()) {
            game.resign(player);
        }
    }
}