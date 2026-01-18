package pt.training.go.server.command;

/**
 * QuitCommand reprezentuje polecenie opuszczenia gry.
 * Gracz wysyła to polecenie, aby się rozłączyć i zakończyć rozgrywkę.
 */
public class QuitCommand implements Command {

    /**
     * Wykonuje operację opuszczenia gry.
     * Wywoła metodę quit w kontekście gry.
     *
     * @param game kontekst gry zawierający stan rozgrywki
     * @param player gracz opuszczający grę
     */
    @Override
    public void execute(GameContext game, PlayerContext player) {
        game.quit(player);
    }
}