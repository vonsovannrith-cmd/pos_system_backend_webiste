package com.example.pos_system.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)      // Null fields are not included in the JSON
public record AuthResponse(

        String token,           // null in Production
        String tokenType,       // null in Production
        String fullName,
        String email,
        String role
) {
}
