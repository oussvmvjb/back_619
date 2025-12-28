package user.biblio4.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Filtre JWT qui intercepte chaque requête pour valider le token
 * et établir le contexte de sécurité Spring
 */
@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain chain) throws ServletException, IOException {
        
        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;
        String role = null;
        String level = null;
        Long userId = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                username = jwtUtil.getUsernameFromToken(jwt);
                role = jwtUtil.getRoleFromToken(jwt);
                level = jwtUtil.getLevelFromToken(jwt);
                userId = jwtUtil.getUserIdFromToken(jwt);
            } catch (Exception e) {
                logger.warn("JWT Token extraction failed: " + e.getMessage());
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (jwtUtil.validateToken(jwt)) {
                // Préparer les autorités basées sur le rôle et le niveau
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                
                // Ajouter le rôle (avec préfixe ROLE_)
                if (role != null) {
                    String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                    authorities.add(new SimpleGrantedAuthority(authority));
                }
                
                // Ajouter le niveau comme autorité supplémentaire
                if (level != null) {
                    authorities.add(new SimpleGrantedAuthority("LEVEL_" + level));
                }
                
                // Créer l'objet d'authentification avec toutes les informations
                UsernamePasswordAuthenticationToken authToken = 
                    new UsernamePasswordAuthenticationToken(
                        username, 
                        null, 
                        authorities
                    );
                
                // Ajouter des détails supplémentaires
                UserDetailsImpl userDetails = new UserDetailsImpl(userId, username, role, level);
                authToken.setDetails(userDetails);
                
                // Configurer les détails de la requête
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // Définir le contexte de sécurité
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        chain.doFilter(request, response);
    }
    
    // Classe interne pour stocker les détails de l'utilisateur
    public static class UserDetailsImpl {
        private final Long userId;
        private final String username;
        private final String role;
        private final String level;
        
        public UserDetailsImpl(Long userId, String username, String role, String level) {
            this.userId = userId;
            this.username = username;
            this.role = role;
            this.level = level;
        }
        
        public Long getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getRole() { return role; }
        public String getLevel() { return level; }
    }
}