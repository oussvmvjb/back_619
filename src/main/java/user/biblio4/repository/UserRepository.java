package user.biblio4.repository;

import user.biblio4.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour l'accès aux données des utilisateurs
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Recherche par nom d'utilisateur
    Optional<User> findByUsername(String username);
    
    // Recherche par email
    Optional<User> findByEmail(String email);
    
    // Vérification d'existence
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    
    // Recherche par nom complet (avec LIKE)
    List<User> findByNomCompletContainingIgnoreCase(String nomComplet);
    
    // Recherche par rôle (enum)
    List<User> findByRole(User.Role role);
    
    // Recherche par niveau (enum)
    List<User> findByLevel(User.Level level);
    
    // Recherche par rôle et niveau
    List<User> findByRoleAndLevel(User.Role role, User.Level level);
    
    // Recherche avancée avec plusieurs critères
    @Query("SELECT u FROM User u WHERE " +
           "(:username IS NULL OR u.username LIKE %:username%) AND " +
           "(:email IS NULL OR u.email LIKE %:email%) AND " +
           "(:nomComplet IS NULL OR u.nomComplet LIKE %:nomComplet%) AND " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:level IS NULL OR u.level = :level)")
    List<User> searchUsers(
        @Param("username") String username,
        @Param("email") String email,
        @Param("nomComplet") String nomComplet,
        @Param("role") User.Role role,
        @Param("level") User.Level level
    );
    
    // Compter les utilisateurs par rôle
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    Long countByRole(@Param("role") User.Role role);
    
    // Compter les utilisateurs par niveau
    @Query("SELECT COUNT(u) FROM User u WHERE u.level = :level")
    Long countByLevel(@Param("level") User.Level level);
    
    // Trouver les utilisateurs créés après une certaine date
    List<User> findByCreatedAtAfter(java.time.LocalDateTime date);
    
    // Trouver les utilisateurs créés entre deux dates
    List<User> findByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
}