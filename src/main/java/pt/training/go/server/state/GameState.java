package pt.training.go.server.state;

import pt.training.go.server.command.GameContext;
import pt.training.go.server.command.PlayerContext;

public interface GameState {
    void move(GameContext game, PlayerContext player, int row, int col);
    void pass(GameContext game, PlayerContext player);
    void requestResume(GameContext game, PlayerContext player);
    void agreeEnd(GameContext game, PlayerContext player);
    void toggleDead(GameContext game, PlayerContext player, int row, int col);
    void resign(GameContext game, PlayerContext player);
    void quit(GameContext game, PlayerContext player);
    String getName();
}