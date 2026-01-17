package pt.training.go.server.command;

public class AgreeEndCommand implements Command {

    @Override
    public void execute(GameContext game, PlayerContext player) {
        synchronized (game.lock()) {
            game.agreeEnd(player);
        }
    }
}