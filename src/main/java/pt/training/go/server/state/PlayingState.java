package pt.training.go.server.state;

import pt.training.go.server.command.GameContext;
import pt.training.go.server.command.PlayerContext;

public class PlayingState implements GameState {

    @Override
    public void move(GameContext game, PlayerContext player, int row, int col) {
        if (!game.isCurrentPlayer(player)) {
            player.send("MESSAGE To nie jest twoj ruch.");
            return;
        }

        try {
            game.getBoard().move(row, col, player.getColor());
           
            game.resetPasses();
           
            player.send("MOVE_ACCEPTED " + (row + 1) + " " + (col + 1));
            game.broadcast("BOARD " + game.boardFlat());
           
            if (player.getOpponent() != null) {
                player.getOpponent().send("OPPONENT_MOVED " + (row + 1) + " " + (col + 1));
            }

            game.switchTurn();
           
        } catch (IllegalArgumentException e) {
            player.send("MESSAGE Niedozwolony ruch: " + e.getMessage());
            player.send("YOUR_MOVE");
        }
    }

    @Override
    public void pass(GameContext game, PlayerContext player) {
        if (!game.isCurrentPlayer(player)) {
            player.send("MESSAGE To nie jest twoj ruch.");
            return;
        }

        game.incrementPasses();
        player.send("MESSAGE Spasowales.");
       
        if (player.getOpponent() != null) {
            player.getOpponent().send("MESSAGE Przeciwnik spasowal.");
        }

        if (game.getConsecutivePasses() >= 2) {
            game.broadcast("MESSAGE Faza gry zakonczona. Przechodzimy do ustalania wynikow (SCORING).");
            game.setState(new ScoringState());
        } else {
            game.switchTurn();
        }
    }

    @Override
    public void requestResume(GameContext game, PlayerContext player) {
        player.send("MESSAGE Gra trwa. Nie mozna teraz wznowic.");
        if (game.isCurrentPlayer(player)) {
            player.send("YOUR_MOVE");
        }
    }

    @Override
    public void agreeEnd(GameContext game, PlayerContext player) {
        player.send("MESSAGE Gra trwa. Nie mozna teraz zaakceptowac wyniku.");
        if (game.isCurrentPlayer(player)) {
            player.send("YOUR_MOVE");
        }
    }

    @Override
    public void toggleDead(GameContext game, PlayerContext player, int row, int col) {
        player.send("MESSAGE Gra trwa. Nie mozna teraz oznaczac martwych grup.");
        if (game.isCurrentPlayer(player)) {
            player.send("YOUR_MOVE");
        }
    }

    @Override
    public void resign(GameContext game, PlayerContext player) {
        if (player.getOpponent() != null) {
            player.getOpponent().send("MESSAGE [GAME OVER] Przeciwnik poddal gre. Wygrales!");
        }
        player.send("MESSAGE [GAME OVER] Poddales gre. Przegrales.");
        game.setState(new FinishedState());
    }

    @Override
    public void quit(GameContext game, PlayerContext player) {
        game.broadcast("MESSAGE Gracz " + player.getColor() + " opuscil gre.");
        game.setState(new FinishedState());
    }

    @Override
    public String getName() {
        return "PLAYING";
    }
}