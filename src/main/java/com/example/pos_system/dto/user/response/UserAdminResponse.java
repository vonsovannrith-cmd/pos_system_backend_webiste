package com.example.pos_system.dto.user.response;

import java.time.LocalDateTime;

public record UserAdminResponse(

        Long id,
        String fullName,
        String email,
        String role,
        boolean active,
        LocalDateTime createdAt
) {
}
