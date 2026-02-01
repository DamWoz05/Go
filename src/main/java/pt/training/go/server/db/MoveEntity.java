package pt.training.go.server.db;

import jakarta.persistence.*;

@Entity
@Table(name = "moves")
public class MoveEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long gameId;
    
    private int moveNumber;
    
    private int rowCoord;
    
    private int colCoord; 
    
    private String color;  // CZARNY/BIALY
    
    private String type;   // MOVE/PASS/RESIGN
    
    public MoveEntity() {}
    
    public MoveEntity(Long gameId, int moveNumber, int rowCoord, int colCoord, String color, String type) {
        this.gameId = gameId;
        this.moveNumber = moveNumber;
        this.rowCoord = rowCoord;
        this.colCoord = colCoord;
        this.color = color;
        this.type = type;
    }
    
    // Getters и Setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getGameId() {
        return gameId;
    }
    
    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }
    
    public int getMoveNumber() {
        return moveNumber;
    }
    
    public void setMoveNumber(int moveNumber) {
        this.moveNumber = moveNumber;
    }
    
    public int getRowCoord() {
        return rowCoord;
    }
    
    public void setRowCoord(int rowCoord) {
        this.rowCoord = rowCoord;
    }
    
    public int getColCoord() {
        return colCoord;
    }
    
    public void setColCoord(int colCoord) {
        this.colCoord = colCoord;
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
}