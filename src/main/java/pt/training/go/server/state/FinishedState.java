package pt.training.go.server.state;

import pt.training.go.server.command.GameContext;
import pt.training.go.server.command.PlayerContext;

/**
 * FinishedState reprezentuje stan gry po jej zakończeniu.
 * W tym stanie gracze nie mogą wykonywać ruchów ani innych akcji mających wpływ na grę.
 */
public class FinishedState implements GameState {

    /**
     * Uniemożliwia wykonanie ruchu w ukończonej grze.
     *
     * @param game kontekst gry
     * @param player gracz próbujący wykonać ruch
     * @param row wiersz ruchu
     * @param col kolumna ruchu
     */
    @Override
    public void move(GameContext game, PlayerContext player, int row, int col) {
        player.send("MESSAGE Gra jest zakonczona. Nie mozna wykonywac ruchow.");
    }

    /**
     * Informuje gracza, że gra jest zakończona.
     *
     * @param game kontekst gry
     * @param player gracz próbujący przejść
     */
    @Override
    public void pass(GameContext game, PlayerContext player) {
        player.send("MESSAGE Gra zakonczona.");
    }

    /**
     * Informuje gracza, że gra jest zakończona.
     *
     * @param game kontekst gry
     * @param player gracz próbujący wznowić grę
     */
    @Override
    public void requestResume(GameContext game, PlayerContext player) {
        player.send("MESSAGE Gra zakonczona.");
    }

    /**
     * Informuje gracza, że gra jest zakończona.
     *
     * @param game kontekst gry
     * @param player gracz wyrażający zgodę
     */
    @Override
    public void agreeEnd(GameContext game, PlayerContext player) {
        player.send("MESSAGE Gra zakonczona.");
    }

    /**
     * Informuje gracza, że gra jest zakończona.
     *
     * @param game kontekst gry
     * @param player gracz próbujący przełączyć martwy kamień
     * @param row wiersz kamienia
     * @param col kolumna kamienia
     */
    @Override
    public void toggleDead(GameContext game, PlayerContext player, int row, int col) {
        player.send("MESSAGE Gra zakonczona.");
    }

    /**
     * Informuje gracza, że gra jest zakończona.
     *
     * @param game kontekst gry
     * @param player gracz rezygnujący
     */
    @Override
    public void resign(GameContext game, PlayerContext player) {
        player.send("MESSAGE Gra zakonczona.");
    }

    /**
     * Umożliwia graczowi opuszczenie ukończonej gry.
     *
     * @param game kontekst gry
     * @param player gracz opuszczający grę
     */
    @Override
    public void quit(GameContext game, PlayerContext player) {
    }

    /**
     * Zwraca nazwę tego stanu gry.
     *
     * @return nazwa stanu "FINISHED"
     */
    @Override
    public String getName() {
        return "FINISHED";
    }
}