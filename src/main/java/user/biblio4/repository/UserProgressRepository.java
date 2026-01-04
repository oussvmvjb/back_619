package user.biblio4.repository;

import user.biblio4.model.UserProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserProgressRepository extends JpaRepository<UserProgress, Long> {
    
    // Trouver par userId et levelNumber (Integer, pas LocalDateTime)
    Optional<UserProgress> findByUserIdAndLevelNumber(Long userId, Integer levelNumber);
    
    // Trouver tous les progrès d'un utilisateur
    List<UserProgress> findByUserId(Long userId);
    
    // Trouver les progrès récents après une certaine date
    @Query("SELECT up FROM UserProgress up WHERE up.user.id = :userId AND up.lastAttempt >= :date")
    List<UserProgress> findByUserIdAndLastAttemptAfter(@Param("userId") Long userId, 
                                                      @Param("date") LocalDateTime date);
    
    // Trouver les niveaux complétés
    List<UserProgress> findByUserIdAndQuizPassedTrue(Long userId);
    
    // Trouver les niveaux en cours
    List<UserProgress> findByUserIdAndQuizPassedFalse(Long userId);
    
    // Vérifier si un niveau est débloqué
    boolean existsByUserIdAndLevelNumber(Long userId, Integer levelNumber);
    
    // Compter les niveaux complétés
    @Query("SELECT COUNT(up) FROM UserProgress up WHERE up.user.id = :userId AND up.quizPassed = true")
    Long countCompletedLevelsByUserId(@Param("userId") Long userId);
}