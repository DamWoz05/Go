package pt.training.go.server.command;

public interface Command {
    void execute(GameContext game, PlayerContext player);
}