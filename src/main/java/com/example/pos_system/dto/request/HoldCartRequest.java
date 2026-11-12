package com.example.pos_system.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record HoldCartRequest(

        @Size(max = 60, message = "Identifier too long (maximum 60)")
        String label,

        @NotEmpty(message = "Empty basket")
        @Size(max = 100, message = "Too many items (maximum 100 rows)")
        @Valid
        List<CheckoutItemRequest> items
) {
}
