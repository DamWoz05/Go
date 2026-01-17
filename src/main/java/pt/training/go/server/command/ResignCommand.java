package pt.training.go.server.command;

public class ResignCommand implements Command {

    @Override
    public void execute(GameContext game, PlayerContext player) {
        synchronized (game.lock()) {
            game.resign(player);
        }
    }
}