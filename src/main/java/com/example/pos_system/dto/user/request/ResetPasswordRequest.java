package com.example.pos_system.dto.user.request;

import com.example.pos_system.common.PasswordRules;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequest(

        @NotBlank(message = "Please enter the new password")
        @Pattern(regexp = PasswordRules.REGEX, message = PasswordRules.MESSAGE)

        String newPassword
) {
}
