package user.biblio4.dto;

import lombok.Data;

public class AuthRequest {
    
    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }
    
    @Data
    public static class RegisterRequest {
        private String username;
        private String email;
        private String password;
        private String nomComplet;
        private String role;
        private String level;
    }
    
    @Data
    public static class ChangePasswordRequest {
        private String oldPassword;
        private String newPassword;
    }
    
    @Data
    public static class UpdateProfileRequest {
        private String nomComplet;
        private String email;
        private String level;
    }

    // Adding missing DTOs for Admin User Management
    @Data
    public static class CreateUserRequest {
        private String username;
        private String email;
        private String password;
        private String nomComplet;
        private String role;
        private String level;
    }

    @Data
    public static class UpdateUserRequest {
        private String username;
        private String email;
        private String password;
        private String nomComplet;
        private String role;
        private String level;
    }
}
