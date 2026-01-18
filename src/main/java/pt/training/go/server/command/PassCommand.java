package pt.training.go.server.command;

/**
 * PassCommand reprezentuje polecenie przejścia gracza.
 * Gracz wysyła to polecenie, aby pominąć swoją kolej bez wykonania ruchu.
 */
public class PassCommand implements Command {

    /**
     * Wykonuje operację przejścia gracza.
     * Synchronizuje dostęp do kontekstu gry i wywoła metodę pass.
     *
     * @param game kontekst gry zawierający stan rozgrywki
     * @param player gracz wykonujący przejście
     */
    @Override
    public void execute(GameContext game, PlayerContext player) {
        synchronized (game.lock()) {
            game.pass(player);
        }
    }
}