package user.biblio4.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entité représentant un utilisateur du système de bibliothèque
 * Avec gestion des rôles et niveaux pour différents types d'utilisateurs
 */
@Entity
@Table(name = "utilisateurs", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "username"),
           @UniqueConstraint(columnNames = "email")
       })
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // identifiant unique
    
    @Column(name = "username", nullable = false, length = 50, unique = true)
    private String username; // nom d'utilisateur
    
    @Column(name = "email", nullable = false, length = 100, unique = true)
    private String email; // email
    
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash; // mot de passe hashé
    
    @Column(name = "nom_complet", length = 100)
    private String nomComplet; // Optionnel: nom complet
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role; // Rôles: ADMIN/PROF/STUDENT
    
    @Enumerated(EnumType.STRING)
    @Column(name = "level", length = 20)
    private Level level; // Niveaux: BEGINNER/INTERMEDIATE/ADVANCED
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // date de création automatique
    
    // Constructeurs
    public User() {
        this.createdAt = LocalDateTime.now();
    }
    
    public User(String username, String email, String passwordHash, String nomComplet, Role role, Level level) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.nomComplet = nomComplet;
        this.role = role;
        this.level = level;
        this.createdAt = LocalDateTime.now();
    }
    
    // PrePersist pour automatiser la date de création
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
    
    // Getters et Setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPasswordHash() {
        return passwordHash;
    }
    
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
    
    public String getNomComplet() {
        return nomComplet;
    }
    
    public void setNomComplet(String nomComplet) {
        this.nomComplet = nomComplet;
    }
    
    public Role getRole() {
        return role;
    }
    
    public void setRole(Role role) {
        this.role = role;
    }
    
    public Level getLevel() {
        return level;
    }
    
    public void setLevel(Level level) {
        this.level = level;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    // Énumérations pour les rôles et niveaux
    public enum Role {
        ADMIN,
        PROF,
        STUDENT
    }
    
    public enum Level {
        BEGINNER,
        INTERMEDIATE,
        ADVANCED
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        
        User user = (User) o;
        
        if (getId() != null ? !getId().equals(user.getId()) : user.getId() != null) return false;
        return getUsername() != null ? getUsername().equals(user.getUsername()) : user.getUsername() == null;
    }
    
    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getUsername() != null ? getUsername().hashCode() : 0);
        return result;
    }
    
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", nomComplet='" + (nomComplet != null ? nomComplet : "") + '\'' +
                ", role=" + role +
                ", level=" + level +
                ", createdAt=" + createdAt +
                '}';
    }
}