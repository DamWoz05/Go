package pt.training.go.server.command;

/**
 * RequestResumeCommand reprezentuje polecenie żądania wznowienia gry.
 * Gracz wysyła to polecenie, aby zaproponować wznowienie rozgrywki.
 */
public class RequestResumeCommand implements Command {

    /**
     * Wykonuje operację żądania wznowienia gry.
     * Synchronizuje dostęp do kontekstu gry i wywoła metodę requestResume.
     *
     * @param game kontekst gry zawierający stan rozgrywki
     * @param player gracz żądający wznowienia
     */
    @Override
    public void execute(GameContext game, PlayerContext player) {
        synchronized (game.lock()) {
            game.requestResume(player);
        }
    }
}