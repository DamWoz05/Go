package pt.training.go.server.command;

public class PassCommand implements Command {

    @Override
    public void execute(GameContext game, PlayerContext player) {
        synchronized (game.lock()) {
            game.pass(player);
        }
    }
}