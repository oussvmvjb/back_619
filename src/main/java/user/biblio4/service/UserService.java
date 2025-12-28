package user.biblio4.service;

import user.biblio4.model.User;
import user.biblio4.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    public User createUser(User user) {
        System.out.println("=== DEBUG UserService.createUser() START ===");
        System.out.println("Username: " + user.getUsername());
        System.out.println("PasswordHash received: " + user.getPasswordHash());
        System.out.println("Is BCrypt hash? " + 
            (user.getPasswordHash() != null && 
             (user.getPasswordHash().startsWith("$2a$") || 
              user.getPasswordHash().startsWith("$2b$"))));
        
        // Vérifier l'unicité
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Un utilisateur avec ce nom d'utilisateur existe déjà");
        }
        
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Un utilisateur avec cet email existe déjà");
        }
        
        // CORRECTION IMPORTANTE : 
        // NE PAS re-hasher si c'est déjà un hash BCrypt!
        String passwordHash = user.getPasswordHash();
        
        if (passwordHash != null && 
            !passwordHash.startsWith("$2a$") && 
            !passwordHash.startsWith("$2b$")) {
            // Seulement hasher si ce n'est PAS un hash BCrypt
            System.out.println("WARNING: Password is not hashed! Hashing now...");
            passwordHash = passwordEncoder.encode(passwordHash);
            user.setPasswordHash(passwordHash);
        } else {
            System.out.println("Password is already hashed, keeping as is.");
            // Le hash est déjà correct, ne rien faire
        }
        
        // S'assurer que le niveau est défini
        if (user.getLevel() == null) {
            user.setLevel(User.Level.BEGINNER);
        }
        
        User savedUser = userRepository.save(user);
        System.out.println("User saved with ID: " + savedUser.getId());
        System.out.println("Final password hash in DB: " + savedUser.getPasswordHash());
        System.out.println("=== DEBUG UserService.createUser() END ===");
        
        return savedUser;
    }
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    public User updateUser(Long id, User userDetails) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID : " + id));
        
        // Vérifier l'unicité du nom d'utilisateur si modifié
        if (!user.getUsername().equals(userDetails.getUsername()) && 
            userRepository.existsByUsername(userDetails.getUsername())) {
            throw new RuntimeException("Un utilisateur avec ce nom d'utilisateur existe déjà");
        }
        
        // Vérifier l'unicité de l'email si modifié
        if (!user.getEmail().equals(userDetails.getEmail()) && 
            userRepository.existsByEmail(userDetails.getEmail())) {
            throw new RuntimeException("Un utilisateur avec cet email existe déjà");
        }
        
        // Mettre à jour les champs
        user.setNomComplet(userDetails.getNomComplet());
        user.setEmail(userDetails.getEmail());
        user.setUsername(userDetails.getUsername());
        
        // Si un nouveau mot de passe est fourni (non hashé), le hasher
        if (userDetails.getPasswordHash() != null && !userDetails.getPasswordHash().isEmpty()) {
            // Vérifier si le mot de passe est déjà hashé (contient $2a$ ou $2b$)
            String passwordHash = userDetails.getPasswordHash();
            if (!passwordHash.startsWith("$2a$") && !passwordHash.startsWith("$2b$")) {
                // Ce n'est pas un hash BCrypt, donc le hasher
                passwordHash = passwordEncoder.encode(passwordHash);
            }
            user.setPasswordHash(passwordHash);
        }
        
        user.setRole(userDetails.getRole());
        user.setLevel(userDetails.getLevel());
        
        return userRepository.save(user);
    }
    
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Utilisateur non trouvé avec l'ID : " + id);
        }
        userRepository.deleteById(id);
    }
    
    public List<User> searchUsersByNomComplet(String nomComplet) {
        return userRepository.findByNomCompletContainingIgnoreCase(nomComplet);
    }
    
    public List<User> getUsersByRole(String role) {
        try {
            User.Role roleEnum = User.Role.valueOf(role.toUpperCase());
            return userRepository.findByRole(roleEnum);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Rôle invalide: " + role);
        }
    }
    
    public List<User> getUsersByLevel(String level) {
        try {
            User.Level levelEnum = User.Level.valueOf(level.toUpperCase());
            return userRepository.findByLevel(levelEnum);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Niveau invalide: " + level);
        }
    }
    
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }
    
    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }
    
    public Optional<User> authenticate(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(password, user.getPasswordHash())) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }
    
    public Long countByRole(User.Role role) {
        return userRepository.countByRole(role);
    }
    
    public Long countByLevel(User.Level level) {
        return userRepository.countByLevel(level);
    }
    
    public List<User> searchUsers(String username, String email, String nomComplet, User.Role role, User.Level level) {
        return userRepository.searchUsers(username, email, nomComplet, role, level);
    }
    
    public List<User> getUsersCreatedAfter(LocalDateTime date) {
        return userRepository.findByCreatedAtAfter(date);
    }
    
    public List<User> getUsersCreatedBetween(LocalDateTime start, LocalDateTime end) {
        return userRepository.findByCreatedAtBetween(start, end);
    }
}