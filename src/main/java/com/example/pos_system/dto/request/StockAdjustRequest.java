package com.example.pos_system.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StockAdjustRequest(

        @NotNull(message = "Please enter the new quantity")
        @Min(value = 0, message = "Stock can't be negative")
        Integer newQuantity,

        @NotBlank(message = "Please enter the reason")
        @Size(max = 255, message = "The reason too long")
        String reason
) {
}
