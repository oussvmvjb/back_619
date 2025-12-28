package user.biblio4.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Utilitaire complet pour la gestion des tokens JWT
 * Fournit des méthodes pour générer, extraire et valider les tokens
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret:your-secret-key-for-jwt-generation-min-256-bits}")
    private String secret;

    @Value("${jwt.expiration:3600000}") // 1 heure par défaut
    private Long expiration;

    private SecretKey getSigningKey() {
        // Utiliser une clé de 256 bits minimum pour HS256
        byte[] keyBytes = secret.getBytes();
        if (keyBytes.length < 32) {
            // Padding si la clé est trop courte
            byte[] paddedKey = new byte[32];
            System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 32));
            keyBytes = paddedKey;
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public String extractLevel(String token) {
        return extractClaim(token, claims -> claims.get("level", String.class));
    }

    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    public Boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }

    // Méthode pour générer le token JWT avec toutes les informations
    public String generateToken(Long userId, String username, String role, String level) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        claims.put("level", level != null ? level : "BEGINNER");
        
        return createToken(claims, username);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Méthodes utilitaires pour le contrôleur
    public String getUsernameFromToken(String token) {
        return extractUsername(token);
    }

    public String getRoleFromToken(String token) {
        return extractRole(token);
    }

    public String getLevelFromToken(String token) {
        return extractLevel(token);
    }

    public Long getUserIdFromToken(String token) {
        return extractUserId(token);
    }
}