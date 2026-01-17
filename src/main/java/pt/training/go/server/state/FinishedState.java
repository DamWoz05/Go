package pt.training.go.server.state;

import pt.training.go.server.command.GameContext;
import pt.training.go.server.command.PlayerContext;

public class FinishedState implements GameState {

    @Override
    public void move(GameContext game, PlayerContext player, int row, int col) {
        player.send("MESSAGE Gra jest zakonczona. Nie mozna wykonywac ruchow.");
    }

    @Override
    public void pass(GameContext game, PlayerContext player) {
        player.send("MESSAGE Gra zakonczona.");
    }

    @Override
    public void requestResume(GameContext game, PlayerContext player) {
        player.send("MESSAGE Gra zakonczona.");
    }

    @Override
    public void agreeEnd(GameContext game, PlayerContext player) {
        player.send("MESSAGE Gra zakonczona.");
    }

    @Override
    public void toggleDead(GameContext game, PlayerContext player, int row, int col) {
        player.send("MESSAGE Gra zakonczona.");
    }

    @Override
    public void resign(GameContext game, PlayerContext player) {
        player.send("MESSAGE Gra zakonczona.");
    }

    @Override
    public void quit(GameContext game, PlayerContext player) {
    }

    @Override
    public String getName() {
        return "FINISHED";
    }
}