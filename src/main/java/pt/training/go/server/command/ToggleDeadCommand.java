package pt.training.go.server.command;

public class ToggleDeadCommand implements Command {

    private final int row;
    private final int col;

    public ToggleDeadCommand(int row, int col) {
        this.row = row;
        this.col = col;
    }

    @Override
    public void execute(GameContext game, PlayerContext player) {
        synchronized (game.lock()) {
            game.toggleDeadCommand(row, col, player);
        }
    }
}