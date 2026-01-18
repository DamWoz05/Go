package pt.training.go.server.state;

import pt.training.go.server.Board;
import pt.training.go.server.command.GameContext;
import pt.training.go.server.command.PlayerContext;

/**
 * ScoringState reprezentuje stan gry podczas liczenia wyniku.
 * W tym stanie gracze mogą oznaczać martwe kamienie i wyrażać zgodę na wynik,
 * lub wznowić grę jeśli się nie zgadzają.
 */
public class ScoringState implements GameState {

    private boolean blackAgreed = false;
    private boolean whiteAgreed = false;

    /**
     * Uniemożliwia wykonanie ruchu w stanie liczenia wyniku.
     *
     * @param game kontekst gry
     * @param player gracz próbujący wykonać ruch
     * @param row wiersz ruchu
     * @param col kolumna ruchu
     */
    @Override
    public void move(GameContext game, PlayerContext player, int row, int col) {
        player.send("MESSAGE [SCORING] Gra jest zatrzymana. Uzyj: dead, agree albo resume.");
    }

    /**
     * Uniemożliwia przejście w stanie liczenia wyniku.
     *
     * @param game kontekst gry
     * @param player gracz próbujący przejść
     */
    @Override
    public void pass(GameContext game, PlayerContext player) {
        player.send("MESSAGE [SCORING] Gra jest zatrzymana. Uzyj: dead, agree albo resume.");
    }

    /**
     * Obsługuje żądanie wznowienia gry w stanie liczenia wyniku.
     * Wyczyść oznaczenia martwych kamieni i powraca do stanu PlayingState.
     *
     * @param game kontekst gry
     * @param player gracz żądający wznowienia
     */
    @Override
    public void requestResume(GameContext game, PlayerContext player) {
        game.broadcast("MESSAGE [INFO] Brak zgody co do wynikow. Gracz " + player.getColor() + " zazadal wznowienia gry.");
       
        game.clearDeadMarks();
        game.resetPasses();
        game.setState(new PlayingState());
       
        game.setCurrentPlayer(player);
        player.send("MESSAGE Wznowiles gre. Ruszasz jako pierwszy.");
        player.send("YOUR_MOVE");
       
        if (player.getOpponent() != null) {
            player.getOpponent().send("MESSAGE Przeciwnik wznowil gre. Czekaj na jego ruch.");
        }
    }

    /**
     * Obsługuje wyrażenie zgody na wynik.
     * Gdy obaj gracze się zgodzą, oblicza wynik i kończy grę.
     *
     * @param game kontekst gry
     * @param player gracz wyrażający zgodę
     */
    @Override
    public void agreeEnd(GameContext game, PlayerContext player) {
        if (player.getColor().toString().equals("CZARNY")) {
            if (blackAgreed) {
                player.send("MESSAGE [SCORING] Juz zaakceptowales wynik.");
                return;
            }
            blackAgreed = true;
        } else {
            if (whiteAgreed) {
                player.send("MESSAGE [SCORING] Juz zaakceptowales wynik.");
                return;
            }
            whiteAgreed = true;
        }

        if (player.getOpponent() != null) {
            player.getOpponent().send("MESSAGE [SCORING] Przeciwnik zaakceptowal wynik. Czeka na twoja zgode.");
        }
        player.send("MESSAGE [SCORING] Zaakceptowales wynik. Czekaj na przeciwnika.");

        if (blackAgreed && whiteAgreed) {
            game.applyDeadMarks();

            Board.GameResult result = game.getBoard().calculateResult();
           
            game.broadcast("MESSAGE " + result.toString());
           
            String winner;
            if (result.blackTotal > result.whiteTotal) {
                winner = "WYGRAL CZARNY (roznica: " + (result.blackTotal - result.whiteTotal) + ")";
            } else if (result.whiteTotal > result.blackTotal) {
                winner = "WYGRAL BIALY (roznica: " + (result.whiteTotal - result.blackTotal) + ")";
            } else {
                winner = "REMIS!";
            }
           
            game.broadcast("MESSAGE [GAME OVER] " + winner);
            game.setState(new FinishedState());
        }
    }

    /**
     * Obsługuje przełączenie martwości kamienia.
     * Umożliwia graczom oznaczanie kamieni jako martwych.
     *
     * @param game kontekst gry
     * @param player gracz przełączający martwość
     * @param row wiersz kamienia
     * @param col kolumna kamienia
     */
    @Override
    public void toggleDead(GameContext game, PlayerContext player, int row, int col) {
        game.toggleDead(row, col, player);
    }

    /**
     * Obsługuje rezygnację gracza podczas liczenia wyniku.
     * Kończy grę i przechodzi do stanu FinishedState.
     *
     * @param game kontekst gry
     * @param player gracz rezygnujący
     */
    @Override
    public void resign(GameContext game, PlayerContext player) {
        if (player.getOpponent() != null) {
            player.getOpponent().send("MESSAGE [GAME OVER] Przeciwnik poddal gre. Wygrales!");
        }
        player.send("MESSAGE [GAME OVER] Poddales gre. Przegrales.");
        game.setState(new FinishedState());
    }

    /**
     * Obsługuje opuszczenie gry przez gracza podczas liczenia wyniku.
     * Kończy grę i przechodzi do stanu FinishedState.
     *
     * @param game kontekst gry
     * @param player gracz opuszczający grę
     */
    @Override
    public void quit(GameContext game, PlayerContext player) {
        game.broadcast("MESSAGE Gracz " + player.getColor() + " opuscil gre podczas liczenia.");
        game.setState(new FinishedState());
    }

    /**
     * Zwraca nazwę tego stanu gry.
     *
     * @return nazwa stanu "SCORING"
     */
    @Override
    public String getName() {
        return "SCORING";
    }
}