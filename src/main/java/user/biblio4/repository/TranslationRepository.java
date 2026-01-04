package user.biblio4.repository;

import user.biblio4.model.Translation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TranslationRepository extends JpaRepository<Translation, Long> {
    
    Optional<Translation> findByWordKeyAndLanguageCode(String wordKey, String languageCode);
    
    List<Translation> findByWordKey(String wordKey);
    
    List<Translation> findByLanguageCode(String languageCode);
    List<Translation> findByLanguageCodeAndTextContainingIgnoreCase(String languageCode, String query);
    
    @Query("SELECT t FROM Translation t WHERE t.text LIKE %:query% AND t.languageCode = :language")
    List<Translation> findByTextContainingAndLanguageCode(
            @Param("query") String query, 
            @Param("language") String language);
}