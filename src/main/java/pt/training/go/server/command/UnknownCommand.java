package pt.training.go.server.command;

public class UnknownCommand implements Command {
    private final String raw;

    public UnknownCommand(String raw) {
        this.raw = raw;
    }

    @Override
    public void execute(GameContext game, PlayerContext player) {
        player.send("MESSAGE Unknown command: " + raw);
        synchronized (game.lock()) {
            if (game.isCurrentPlayer(player)) {
                player.send("YOUR_MOVE");
            }
        }
    }
}