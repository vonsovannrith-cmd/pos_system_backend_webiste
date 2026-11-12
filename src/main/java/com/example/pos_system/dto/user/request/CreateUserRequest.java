package com.example.pos_system.dto.user.request;

import com.example.pos_system.common.PasswordRules;
import com.example.pos_system.model.entity.enums.Role;
import jakarta.validation.constraints.*;

public record CreateUserRequest(

        @NotBlank(message = "Please enter the name")
        @Size(max = 100, message = "Name is too long")
        String fullName,

        @NotBlank(message = "Please enter the email")
        @Email(message = "Email is Invalid")
        @Size(max = 150, message = "Email is too long")
        String email,

        @NotBlank(message = "Please enter the password")
        @Pattern(regexp = PasswordRules.REGEX, message = PasswordRules.MESSAGE)
        String password,

        @NotNull(message = "Please chooses role")
        Role role
) {
}
