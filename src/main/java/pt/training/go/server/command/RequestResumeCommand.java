package pt.training.go.server.command;

public class RequestResumeCommand implements Command {

    @Override
    public void execute(GameContext game, PlayerContext player) {
        synchronized (game.lock()) {
            game.requestResume(player);
        }
    }
}