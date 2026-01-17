package pt.training.go.server.command;

import pt.training.go.server.StoneColor;

public interface PlayerContext {
    StoneColor getColor();
    PlayerContext getOpponent();
    void send(String line);
    void requestStop();
}