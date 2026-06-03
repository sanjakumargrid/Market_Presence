package com.griddynamics.forge.market_presence_service.dto;

import lombok.*;

public class AuthDto {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LoginRequest {
        private String email;
        private String password;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RegisterRequest {
        private String email;
        private String password;
        private String name;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AuthResponse {
        private String token;
        private String email;
        private String role;
        private String userId;
        private String name;
    }
}
