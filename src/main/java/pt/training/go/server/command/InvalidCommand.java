package pt.training.go.server.command;

public class InvalidCommand implements Command {

    private final String message;

    public InvalidCommand(String message) {
        this.message = message;
    }

    @Override
    public void execute(GameContext game, PlayerContext player) {
        player.send("MESSAGE " + message);
        synchronized (game.lock()) {
            if (game.isCurrentPlayer(player)) {
                player.send("YOUR_MOVE");
            }
        }
    }
}