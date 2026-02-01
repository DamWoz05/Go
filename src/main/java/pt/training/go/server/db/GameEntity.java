package pt.training.go.server.db;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "games")
public class GameEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private LocalDateTime startTime;
    
    private int boardSize;
    
    private String winner; // CZARNY/BIALY/REMIS - nullable
    
    public GameEntity() {}
    
    public GameEntity(int boardSize) {
        this.boardSize = boardSize;
        this.startTime = LocalDateTime.now();
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public int getBoardSize() {
        return boardSize;
    }
    
    public void setBoardSize(int boardSize) {
        this.boardSize = boardSize;
    }
    
    public String getWinner() {
        return winner;
    }
    
    public void setWinner(String winner) {
        this.winner = winner;
    }
}