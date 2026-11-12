package com.example.pos_system.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StockInRequest(

        @NotNull(message = "Please enter the quantity")
        @Min(value = 1, message = "\n" +
                "The quantity must be greater than 0")
        Integer quantity,

        @Size(max = 255, message = "The reason too long")
        String reason
) {
}
