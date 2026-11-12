package com.example.pos_system.dto.user.request;

import com.example.pos_system.model.entity.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(

        @NotBlank(message = "Please enter the full name")
        @Size(max = 100, message = "Full name is too long")
        String fullName,

        @NotNull(message = "Please chooses role")
        Role role,

        @NotNull(message = "Please define status")
        Boolean active
) {
}
