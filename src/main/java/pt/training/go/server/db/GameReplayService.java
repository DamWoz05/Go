package pt.training.go.server.db;

import org.springframework.stereotype.Service;
import pt.training.go.server.Board;
import pt.training.go.server.StoneColor;

import java.util.List;

/**
 * Serwis do odtwarzania gier dla GUI.
 * Używany przez GUI do nawigacji ruch po ruchu.
 */
@Service
public class GameReplayService {
    
    private final MoveRepository moveRepository;
    private final GameRepository gameRepository;
    
    public GameReplayService(MoveRepository moveRepository, GameRepository gameRepository) {
        this.moveRepository = moveRepository;
        this.gameRepository = gameRepository;
    }
    
    /**
     * Pobiera wszystkie gry z bazy.
     *
     * @return lista wszystkich gier
     */
    public List<GameEntity> getAllGames() {
        return gameRepository.findAll();
    }
    
    /**
     * Pobiera wszystkie ruchy dla danej gry.
     *
     * @param gameId ID gry
     * @return lista ruchów posortowana chronologicznie
     */
    public List<MoveEntity> getMovesForGame(Long gameId) {
        return moveRepository.findByGameIdOrderByMoveNumberAsc(gameId);
    }
    
    /**
     * Rekonstruuje stan planszy po wykonaniu N ruchów.
     *
     * @param gameId ID gry
     * @param moveIndex indeks ruchu (0-based), -1 = początek gry
     * @param boardSize rozmiar planszy
     * @return zrekonstruowana plansza
     */
    public Board reconstructBoardAtMove(Long gameId, int moveIndex, int boardSize) {
        Board board = new Board(boardSize);
        List<MoveEntity> moves = getMovesForGame(gameId);
        
        // Odtwarzamy ruchy do podanego indeksu
        for (int i = 0; i <= moveIndex && i < moves.size(); i++) {
            MoveEntity move = moves.get(i);
            
            if ("MOVE".equals(move.getType())) {
                StoneColor color = "CZARNY".equals(move.getColor()) 
                    ? StoneColor.CZARNY 
                    : StoneColor.BIALY;
                
                board.forcePlaceStone(move.getRowCoord(), move.getColCoord(), color);
            }
            // PASS i RESIGN nie zmieniają planszy
        }
        
        return board;
    }
    
    /**
     * Pobiera grę po ID.
     *
     * @param gameId ID gry
     * @return encja gry lub null jeśli nie istnieje
     */
    public GameEntity getGame(Long gameId) {
        return gameRepository.findById(gameId).orElse(null);
    }
    
    /**
     * Klasa pomocnicza reprezentująca stan replay w danym momencie.
     */
    public static class ReplayState {
        public final Board board;
        public final int currentMoveIndex;
        public final int totalMoves;
        public final String moveDescription;
        public final boolean canGoNext;
        public final boolean canGoPrevious;
        
        public ReplayState(Board board, int currentMoveIndex, int totalMoves, 
                          String moveDescription, boolean canGoNext, boolean canGoPrevious) {
            this.board = board;
            this.currentMoveIndex = currentMoveIndex;
            this.totalMoves = totalMoves;
            this.moveDescription = moveDescription;
            this.canGoNext = canGoNext;
            this.canGoPrevious = canGoPrevious;
        }
    }
    
    /**
     * Tworzy pełny stan replay dla danego ruchu.
     *
     * @param gameId ID gry
     * @param moveIndex indeks ruchu (-1 = początek)
     * @param boardSize rozmiar planszy
     * @return stan replay
     */
    public ReplayState getReplayState(Long gameId, int moveIndex, int boardSize) {
        List<MoveEntity> moves = getMovesForGame(gameId);
        Board board = reconstructBoardAtMove(gameId, moveIndex, boardSize);
        
        String description;
        if (moveIndex < 0) {
            description = "Początek gry";
        } else if (moveIndex < moves.size()) {
            MoveEntity move = moves.get(moveIndex);
            if ("MOVE".equals(move.getType())) {
                description = String.format("Ruch %d: %s na (%d, %d)", 
                    move.getMoveNumber(), move.getColor(), 
                    move.getRowCoord() + 1, move.getColCoord() + 1);
            } else {
                description = String.format("Ruch %d: %s - %s", 
                    move.getMoveNumber(), move.getColor(), move.getType());
            }
        } else {
            description = "Koniec gry";
        }
        
        return new ReplayState(
            board,
            moveIndex,
            moves.size(),
            description,
            moveIndex < moves.size() - 1,
            moveIndex >= -1
        );
    }
}