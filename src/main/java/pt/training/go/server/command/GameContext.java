package pt.training.go.server.command;

import pt.training.go.server.Board;
import pt.training.go.server.state.GameState;

public interface GameContext {
    Object lock();
   
    void setState(GameState state);
   
    // Pozwala wymusić, czyja jest teraz kolej
    void setCurrentPlayer(PlayerContext player);
   
    Board getBoard();
    boolean isCurrentPlayer(PlayerContext player);
    void switchTurn();
    void broadcast(String message);

    int getConsecutivePasses();
    void resetPasses();
    void incrementPasses();

    boolean[][] getDeadMarks();
    void clearDeadMarks();
    void toggleDead(int row, int col, PlayerContext player);
    void toggleDeadCommand(int row, int col, PlayerContext player);
    void applyDeadMarks();
   
    void makeMove(int row, int col, PlayerContext player);
    void pass(PlayerContext player);
    void requestResume(PlayerContext player);
    void agreeEnd(PlayerContext player);
    void resign(PlayerContext player);
    void quit(PlayerContext player);
   
    String boardFlat();
    int getBoardSize();
}