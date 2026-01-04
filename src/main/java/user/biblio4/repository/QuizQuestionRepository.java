package user.biblio4.repository;

import user.biblio4.model.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {
    
    // Trouver les questions par niveau
    List<QuizQuestion> findByLevelNumber(Integer levelNumber);
    
    // Trouver des questions aléatoires pour un niveau
    @Query(value = "SELECT * FROM quiz_questions WHERE level_number = :levelNumber ORDER BY RAND() LIMIT :count", 
           nativeQuery = true)
    List<QuizQuestion> findRandomQuestionsByLevel(@Param("levelNumber") Integer levelNumber, 
                                                  @Param("count") int count);
    
    // Compter les questions par niveau
    long countByLevelNumber(Integer levelNumber);
}