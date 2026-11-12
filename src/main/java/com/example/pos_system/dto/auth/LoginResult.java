package com.example.pos_system.dto.auth;

public record LoginResult(

        String token,
        String fullName,
        String email,
        String role
) {
}
