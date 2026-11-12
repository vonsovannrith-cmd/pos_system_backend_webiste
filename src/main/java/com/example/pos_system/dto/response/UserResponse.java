package com.example.pos_system.dto.response;

public record UserResponse(

        Long id,
        String fullName,
        String email,
        String role
) {
}
