package user.biblio4.repository;

import user.biblio4.model.LevelWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LevelWordRepository extends JpaRepository<LevelWord, Long> {
    
    List<LevelWord> findByLevelNumber(Integer levelNumber);
    
    List<LevelWord> findByLevelNumberOrderByDisplayOrderAsc(Integer levelNumber);
    
    Optional<LevelWord> findByWordKey(String wordKey);
    
    Optional<LevelWord> findByWordKeyAndLevelNumber(String wordKey, Integer levelNumber);
    
    List<LevelWord> findByCategory(String category);
    
    @Query("SELECT COUNT(l) FROM LevelWord l WHERE l.levelNumber = :levelNumber")
    Integer countByLevelNumber(@Param("levelNumber") Integer levelNumber);
}