package com.example.pos_system.dto.auth;

import com.example.pos_system.common.PasswordRules;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangePasswordRequest(

        @NotBlank(message = "Please enter the CurrentPassword")
        String currentPassword,

        @NotBlank(message = "Please enter the new password")
        @Pattern(regexp = PasswordRules.REGEX, message = PasswordRules.MESSAGE)
        String newPassword
) {
}
