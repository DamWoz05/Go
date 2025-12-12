package pt.training.go.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RulesTest {
    private Board board;
    
    @BeforeEach
    public void CreateBoard() {
        board = new Board(19);
    }
    
    // Kamieni nie można przesunac
    @Test
    public void testPlacement() {
        board.move(0, 0, StoneColor.CZARNY);
        assertEquals(StoneColor.CZARNY.asChar(), board.grid[0][0]);
        
        try {
            board.move(0, 0, StoneColor.BIALY);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }
    
    @Test
    public void testCaptureSingleStone() {
        // Ofiara xD
        board.grid[1][1] = StoneColor.BIALY.asChar();
        
        board.grid[0][1] = StoneColor.CZARNY.asChar(); 
        board.grid[2][1] = StoneColor.CZARNY.asChar(); 
        board.grid[1][0] = StoneColor.CZARNY.asChar(); 
        
        board.move(1, 2, StoneColor.CZARNY);
        
        assertEquals(Board.EMPTY, board.grid[1][1]);
        assertEquals(1, board.getBlackPrisoners());
    }
    
    @Test
    public void testCaptureChain() {
        board.grid[1][1] = StoneColor.BIALY.asChar();
        board.grid[1][2] = StoneColor.BIALY.asChar();
        board.grid[0][1] = StoneColor.CZARNY.asChar();
        board.grid[0][2] = StoneColor.CZARNY.asChar();
        board.grid[2][1] = StoneColor.CZARNY.asChar();
        board.grid[2][2] = StoneColor.CZARNY.asChar();
        board.grid[1][0] = StoneColor.CZARNY.asChar();
        
        board.move(1, 3, StoneColor.CZARNY);
        
        assertEquals(Board.EMPTY, board.grid[1][1]);
        assertEquals(Board.EMPTY, board.grid[1][2]);
        assertEquals(2, board.getBlackPrisoners());
    }
    
    // Suicide Rule
    @Test
    public void testSuicideRule() {
        board.grid[0][1] = StoneColor.CZARNY.asChar();
        board.grid[2][1] = StoneColor.CZARNY.asChar();
        board.grid[1][0] = StoneColor.CZARNY.asChar();
        board.grid[1][2] = StoneColor.CZARNY.asChar();
        
        try {
            board.move(1, 1, StoneColor.BIALY);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals(Board.EMPTY, board.grid[1][1]);
        }
    }
}