package user.biblio4.controller;

import user.biblio4.model.User;
import user.biblio4.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import user.biblio4.dto.AuthRequest.ChangePasswordRequest;
import user.biblio4.dto.AuthRequest.CreateUserRequest;
import user.biblio4.dto.AuthRequest.UpdateProfileRequest;
import user.biblio4.dto.AuthRequest.UpdateUserRequest;
import java.util.Map;
import java.util.Optional;

/**
 * Contrôleur REST pour la gestion des utilisateurs
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/welcome")
    public ResponseEntity<String> welcome() {
        return ResponseEntity.ok("Microservice de gestion des utilisateurs - API REST");
    }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getProfile() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();

            Optional<User> userOpt = userService.getUserByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Utilisateur non trouvé"));
            }

            User user = userOpt.get();
            Map<String, Object> response = createUserResponse(user);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la récupération du profil: " + e.getMessage()));
        }
    }

    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfileRequest updateRequest) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();

            Optional<User> userOpt = userService.getUserByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Utilisateur non trouvé"));
            }

            User user = userOpt.get();

            // Mettre à jour les informations
            if (updateRequest.getNomComplet() != null) {
                user.setNomComplet(updateRequest.getNomComplet());
            }

            if (updateRequest.getEmail() != null && !updateRequest.getEmail().equals(user.getEmail())) {
                // Vérifier si l'email n'est pas déjà utilisé
                if (userService.emailExists(updateRequest.getEmail())) {
                    return ResponseEntity.badRequest()
                            .body(createErrorResponse("Cet email est déjà utilisé"));
                }
                user.setEmail(updateRequest.getEmail());
            }

            // Mettre à jour le niveau si fourni
            if (updateRequest.getLevel() != null) {
                try {
                    User.Level level = User.Level.valueOf(updateRequest.getLevel().toUpperCase());
                    user.setLevel(level);
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest()
                            .body(createErrorResponse("Niveau invalide"));
                }
            }

            User updatedUser = userService.updateUser(user.getId(), user);
            Map<String, Object> response = createUserResponse(updatedUser);
            response.put("message", "Profil mis à jour avec succès");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la mise à jour du profil: " + e.getMessage()));
        }
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest changePasswordRequest) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();

            Optional<User> userOpt = userService.getUserByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Utilisateur non trouvé"));
            }

            User user = userOpt.get();

            // Vérifier l'ancien mot de passe
            boolean oldPasswordMatches = userService.authenticate(username, changePasswordRequest.getOldPassword())
                    .isPresent();
            if (!oldPasswordMatches) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Ancien mot de passe incorrect"));
            }

            // Mettre à jour avec le nouveau mot de passe
            user.setPasswordHash(changePasswordRequest.getNewPassword());
            userService.updateUser(user.getId(), user);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Mot de passe changé avec succès");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors du changement de mot de passe: " + e.getMessage()));
        }
    }

    // === Routes ADMIN seulement ===

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la récupération des utilisateurs: " + e.getMessage()));
        }
    }

    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            Optional<User> user = userService.getUserById(id);

            if (user.isPresent()) {
                return ResponseEntity.ok(user.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Utilisateur non trouvé avec l'ID: " + id));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la récupération de l'utilisateur: " + e.getMessage()));
        }
    }

    @PostMapping("/admin/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest createRequest) {
        try {
            // Vérifier les données d'entrée
            if (createRequest.getUsername() == null || createRequest.getUsername().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Le nom d'utilisateur est requis"));
            }

            if (createRequest.getEmail() == null || createRequest.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("L'email est requis"));
            }

            if (createRequest.getPassword() == null || createRequest.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Le mot de passe est requis"));
            }

            // Déterminer le rôle
            User.Role role;
            try {
                role = createRequest.getRole() != null
                        ? User.Role.valueOf(createRequest.getRole().toUpperCase())
                        : User.Role.STUDENT;
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Rôle invalide. Options: ADMIN, PROF, STUDENT"));
            }

            // Déterminer le niveau
            User.Level level;
            try {
                level = createRequest.getLevel() != null
                        ? User.Level.valueOf(createRequest.getLevel().toUpperCase())
                        : User.Level.BEGINNER;
            } catch (IllegalArgumentException e) {
                level = User.Level.BEGINNER;
            }

            // Créer l'utilisateur
            User user = new User(
                    createRequest.getUsername(),
                    createRequest.getEmail(),
                    createRequest.getPassword(), // Le service va hasher
                    createRequest.getNomComplet(),
                    role,
                    level);

            User savedUser = userService.createUser(user);
            Map<String, Object> response = createUserResponse(savedUser);
            response.put("message", "Utilisateur créé avec succès");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la création de l'utilisateur: " + e.getMessage()));
        }
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest updateRequest) {
        try {
            Optional<User> userOpt = userService.getUserById(id);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Utilisateur non trouvé avec l'ID: " + id));
            }

            User user = userOpt.get();

            // Mettre à jour les informations
            if (updateRequest.getNomComplet() != null) {
                user.setNomComplet(updateRequest.getNomComplet());
            }

            if (updateRequest.getEmail() != null && !updateRequest.getEmail().equals(user.getEmail())) {
                if (userService.emailExists(updateRequest.getEmail())) {
                    return ResponseEntity.badRequest()
                            .body(createErrorResponse("Cet email est déjà utilisé"));
                }
                user.setEmail(updateRequest.getEmail());
            }

            if (updateRequest.getUsername() != null && !updateRequest.getUsername().equals(user.getUsername())) {
                if (userService.usernameExists(updateRequest.getUsername())) {
                    return ResponseEntity.badRequest()
                            .body(createErrorResponse("Ce nom d'utilisateur est déjà utilisé"));
                }
                user.setUsername(updateRequest.getUsername());
            }

            if (updateRequest.getRole() != null) {
                try {
                    User.Role role = User.Role.valueOf(updateRequest.getRole().toUpperCase());
                    user.setRole(role);
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest()
                            .body(createErrorResponse("Rôle invalide. Options: ADMIN, PROF, STUDENT"));
                }
            }

            if (updateRequest.getLevel() != null) {
                try {
                    User.Level level = User.Level.valueOf(updateRequest.getLevel().toUpperCase());
                    user.setLevel(level);
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest()
                            .body(createErrorResponse("Niveau invalide. Options: BEGINNER, INTERMEDIATE, ADVANCED"));
                }
            }

            if (updateRequest.getPassword() != null && !updateRequest.getPassword().trim().isEmpty()) {
                user.setPasswordHash(updateRequest.getPassword()); // Le service va hasher
            }

            User updatedUser = userService.updateUser(id, user);
            Map<String, Object> response = createUserResponse(updatedUser);
            response.put("message", "Utilisateur mis à jour avec succès");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la mise à jour de l'utilisateur: " + e.getMessage()));
        }
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Utilisateur supprimé avec succès");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la suppression de l'utilisateur: " + e.getMessage()));
        }
    }

    @GetMapping("/admin/search/username/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        try {
            Optional<User> user = userService.getUserByUsername(username);

            if (user.isPresent()) {
                return ResponseEntity.ok(user.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Utilisateur non trouvé: " + username));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la recherche: " + e.getMessage()));
        }
    }

    @GetMapping("/admin/search/email/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserByEmail(@PathVariable String email) {
        try {
            Optional<User> user = userService.getUserByEmail(email);

            if (user.isPresent()) {
                return ResponseEntity.ok(user.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Utilisateur non trouvé avec l'email: " + email));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la recherche: " + e.getMessage()));
        }
    }

    @GetMapping("/admin/search/name/{nomComplet}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> searchUsersByNomComplet(@PathVariable String nomComplet) {
        try {
            List<User> users = userService.searchUsersByNomComplet(nomComplet);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la recherche: " + e.getMessage()));
        }
    }

    @GetMapping("/admin/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUsersByRole(@PathVariable String role) {
        try {
            List<User> users = userService.getUsersByRole(role);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la recherche par rôle: " + e.getMessage()));
        }
    }

    @GetMapping("/admin/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUsersCount() {
        try {
            List<User> users = userService.getAllUsers();
            Map<String, Object> response = new HashMap<>();
            response.put("total", users.size());

            // Compter par rôle
            long adminCount = users.stream().filter(u -> u.getRole() == User.Role.ADMIN).count();
            long profCount = users.stream().filter(u -> u.getRole() == User.Role.PROF).count();
            long studentCount = users.stream().filter(u -> u.getRole() == User.Role.STUDENT).count();

            response.put("admins", adminCount);
            response.put("profs", profCount);
            response.put("students", studentCount);

            // Compter par niveau
            long beginnerCount = users.stream().filter(u -> u.getLevel() == User.Level.BEGINNER).count();
            long intermediateCount = users.stream().filter(u -> u.getLevel() == User.Level.INTERMEDIATE).count();
            long advancedCount = users.stream().filter(u -> u.getLevel() == User.Level.ADVANCED).count();

            response.put("beginners", beginnerCount);
            response.put("intermediates", intermediateCount);
            response.put("advanced", advancedCount);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors du comptage des utilisateurs: " + e.getMessage()));
        }
    }

    // === Méthodes utilitaires ===

    private Map<String, Object> createUserResponse(User user) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("nomComplet", user.getNomComplet());
        response.put("role", user.getRole());
        response.put("level", user.getLevel() != null ? user.getLevel() : User.Level.BEGINNER);
        response.put("createdAt", user.getCreatedAt());
        return response;
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", message);
        return errorResponse;
};
}