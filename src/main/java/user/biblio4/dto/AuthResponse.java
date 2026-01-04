package user.biblio4.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private Long userId;
    private String username;
    private String email;
    private String nomComplet;
    private String role;
    private String level;
    private LocalDateTime createdAt;
    private String message;
    private Map<String, Object> additionalData;
}
