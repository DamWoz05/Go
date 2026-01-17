package pt.training.go.server.command;

public class QuitCommand implements Command {

    @Override
    public void execute(GameContext game, PlayerContext player) {
        game.quit(player);
    }
}