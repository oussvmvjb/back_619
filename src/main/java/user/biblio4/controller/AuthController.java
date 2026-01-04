package user.biblio4.controller;

import user.biblio4.model.User;
import user.biblio4.security.JwtUtil;
import user.biblio4.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import user.biblio4.dto.AuthRequest.LoginRequest;
import user.biblio4.dto.AuthRequest.RegisterRequest;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            Optional<User> userOpt = userService.getUserByUsername(loginRequest.getUsername());

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Nom d'utilisateur ou mot de passe incorrect"));
            }

            User user = userOpt.get();

            // Vérifier le mot de passe
            boolean passwordMatches = passwordEncoder.matches(
                    loginRequest.getPassword(),
                    user.getPasswordHash());

            if (!passwordMatches) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Nom d'utilisateur ou mot de passe incorrect"));
            }

            // Générer le token JWT avec toutes les informations
            String token = jwtUtil.generateToken(
                    user.getId(),
                    user.getUsername(),
                    user.getRole().name(),
                    user.getLevel() != null ? user.getLevel().name() : "BEGINNER");

            Map<String, Object> response = createUserResponse(user);
            response.put("token", token);
            response.put("message", "Authentification réussie");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de l'authentification: " + e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        try {
            // Vérifier les données d'entrée
            if (registerRequest.getUsername() == null || registerRequest.getUsername().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Le nom d'utilisateur est requis"));
            }

            if (registerRequest.getEmail() == null || registerRequest.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("L'email est requis"));
            }

            if (registerRequest.getPassword() == null || registerRequest.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Le mot de passe est requis"));
            }

            // Valider l'email
            if (!isValidEmail(registerRequest.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Format d'email invalide"));
            }

            // Vérifier si l'utilisateur existe déjà
            if (userService.usernameExists(registerRequest.getUsername())) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Ce nom d'utilisateur est déjà utilisé"));
            }

            if (userService.emailExists(registerRequest.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Cet email est déjà utilisé"));
            }

            // Hasher le mot de passe
            String passwordHash1 = passwordEncoder.encode(registerRequest.getPassword());

            // Déterminer le rôle (par défaut: STUDENT)
            User.Role role;
            try {
                role = registerRequest.getRole() != null
                        ? User.Role.valueOf(registerRequest.getRole().toUpperCase())
                        : User.Role.STUDENT;
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Rôle invalide. Options: ADMIN, PROF, STUDENT"));
            }

            // Déterminer le niveau (par défaut: BEGINNER)
            User.Level level;
            try {
                level = registerRequest.getLevel() != null
                        ? User.Level.valueOf(registerRequest.getLevel().toUpperCase())
                        : User.Level.BEGINNER;
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Niveau invalide. Options: BEGINNER, INTERMEDIATE, ADVANCED"));
            }

            // Créer l'utilisateur
            User user = new User(
                    registerRequest.getUsername(),
                    registerRequest.getEmail(),
                    passwordHash1,
                    registerRequest.getNomComplet(),
                    role,
                    level);

            User savedUser = userService.createUser(user);

            // Générer un token pour le nouvel utilisateur
            String token = jwtUtil.generateToken(
                    savedUser.getId(),
                    savedUser.getUsername(),
                    savedUser.getRole().name(),
                    savedUser.getLevel() != null ? savedUser.getLevel().name() : "BEGINNER");

            Map<String, Object> response = createUserResponse(savedUser);
            response.put("token", token);
            response.put("message", "Utilisateur créé avec succès");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la création de l'utilisateur: " + e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Token manquant ou invalide"));
            }

            String token = authorizationHeader.substring(7);

            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Token expiré ou invalide"));
            }

            // Extraire le username du token
            String username = jwtUtil.getUsernameFromToken(token);

            // Vérifier que l'utilisateur existe toujours
            Optional<User> userOpt = userService.getUserByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Utilisateur non trouvé"));
            }

            User user = userOpt.get();

            // Générer un nouveau token
            String newToken = jwtUtil.generateToken(
                    user.getId(),
                    user.getUsername(),
                    user.getRole().name(),
                    user.getLevel() != null ? user.getLevel().name() : "BEGINNER");

            Map<String, Object> response = createUserResponse(user);
            response.put("token", newToken);
            response.put("message", "Token rafraîchi avec succès");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors du rafraîchissement du token: " + e.getMessage()));
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                return ResponseEntity.ok(createValidationResponse(false, "Token manquant ou invalide"));
            }

            String token = authorizationHeader.substring(7);

            boolean isValid = jwtUtil.validateToken(token);

            if (isValid) {
                // Extraire les informations du token pour la réponse
                Map<String, Object> response = new HashMap<>();
                response.put("valid", true);

                try {
                    response.put("userId", jwtUtil.getUserIdFromToken(token));
                    response.put("username", jwtUtil.getUsernameFromToken(token));
                    response.put("role", jwtUtil.getRoleFromToken(token));
                    response.put("level", jwtUtil.getLevelFromToken(token));
                } catch (Exception e) {
                    // Si extraction échoue, token invalide
                    return ResponseEntity.ok(createValidationResponse(false, "Token invalide"));
                }

                response.put("message", "Token valide");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.ok(createValidationResponse(false, "Token expiré ou invalide"));
            }

        } catch (Exception e) {
            return ResponseEntity.ok(createValidationResponse(false, "Erreur de validation: " + e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            // Dans une implémentation avec JWT, la déconnexion est gérée côté client
            // en supprimant le token. Côté serveur, nous pouvons juste valider que le token
            // était valide.

            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);

                if (jwtUtil.validateToken(token)) {
                    Map<String, String> response = new HashMap<>();
                    response.put("message", "Déconnexion réussie");
                    return ResponseEntity.ok(response);
                }
            }

            return ResponseEntity.ok(createErrorResponse("Token invalide"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la déconnexion: " + e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Token manquant ou invalide"));
            }

            String token = authorizationHeader.substring(7);

            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Token expiré ou invalide"));
            }

            String username = jwtUtil.getUsernameFromToken(token);
            Optional<User> userOpt = userService.getUserByUsername(username);

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Utilisateur non trouvé"));
            }

            User user = userOpt.get();
            Map<String, Object> response = createUserResponse(user);
            response.put("message", "Informations utilisateur récupérées avec succès");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la récupération des informations: " + e.getMessage()));
        }
    }

    // Méthodes utilitaires

    private Map<String, Object> createUserResponse(User user) {
        Map<String, Object> response = new HashMap<>();
        response.put("userId", user.getId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("nomComplet", user.getNomComplet());
        response.put("role", user.getRole().name());
        response.put("level", user.getLevel() != null ? user.getLevel().name() : "BEGINNER");
        response.put("createdAt", user.getCreatedAt());
        return response;
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", message);
        return errorResponse;
    }

    private Map<String, Object> createValidationResponse(boolean valid, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("valid", valid);
        response.put("message", message);
        return response;
    }

}