package com.example.pos_system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelOrderRequest(

        @NotBlank(message = "Please enter reason")
        @Size(max = 200, message = "\n" +
                "Reason is too long (maximum 200)")
        String reason
) {
}
