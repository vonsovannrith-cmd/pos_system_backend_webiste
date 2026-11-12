package com.example.pos_system.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductRequest(

        @NotBlank(message = "Please enter the name")
        @Size(max = 150, message = "Name too long")
        String name,

        @NotBlank(message = "Please enter the sku")
        @Size(max = 50, message = "SKU too long")
        String sku,

        @Size(max = 50, message = "Barcode too long")
        String barCode,

        @NotNull(message = "Please enter the price")
        @DecimalMin(value = "0.0", message = "Values can not be negative")
        BigDecimal price,

        @DecimalMin(value = "0.0", message = "Values can not be negative")
        BigDecimal costPrice,

        @Min(value = 0, message = "Values can not be negative")
        Integer lowStockThreshold,

        Long categoryId,

        Boolean active
) {}
