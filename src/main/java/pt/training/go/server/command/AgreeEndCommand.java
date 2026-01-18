package pt.training.go.server.command;

/**
 * AgreeEndCommand reprezentuje polecenie wyrażenia zgody na zakończenie gry.
 * Gracz wysyła to polecenie, aby zaakceptować propozycję zakończenia rozgrywki.
 */
public class AgreeEndCommand implements Command {

    /**
     * Wykonuje operację zgodzenia się na koniec gry.
     * Synchronizuje dostęp do kontekstu gry i wywoła metodę agreeEnd.
     *
     * @param game kontekst gry zawierający stan rozgrywki
     * @param player kontekst gracza wykonującego polecenie
     */
    @Override
    public void execute(GameContext game, PlayerContext player) {
        synchronized (game.lock()) {
            game.agreeEnd(player);
        }
    }
}