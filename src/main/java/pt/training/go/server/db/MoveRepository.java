package pt.training.go.server.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MoveRepository extends JpaRepository<MoveEntity, Long> {
    
    /**
     * Znajduje wszystkie ruchy dla danej gry, posortowane według numeru ruchu.
     *
     * @param gameId ID gry
     * @return lista ruchów uporządkowana chronologicznie
     */
    List<MoveEntity> findByGameIdOrderByMoveNumberAsc(Long gameId);
}