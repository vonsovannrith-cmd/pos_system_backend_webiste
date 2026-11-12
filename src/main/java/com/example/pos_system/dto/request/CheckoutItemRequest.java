package com.example.pos_system.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CheckoutItemRequest(

        @NotNull(message = "Please enter the product")
        Long productId,

        @NotNull(message = "Please enter the quantity")
        @Min(value = 1, message = "The quantity must be at least 1")
        @Max(value = 9999, message = "Too large a quantity")
        Integer quantity
) {
}
