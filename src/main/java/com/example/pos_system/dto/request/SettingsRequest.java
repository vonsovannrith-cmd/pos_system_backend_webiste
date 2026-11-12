package com.example.pos_system.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record SettingsRequest(

        @NotBlank(message = "Please enter store name ")
        @Size(max = 100, message = "Store name is too long")
        String storeName,

        @Size(max = 255, message = "Address is too long")
        String address,

        @Size(max = 30, message = "Phone is too long")
        String phone,

        @NotNull(message = "Please enter tax rate")
        @DecimalMin(value = "0.0", message = "\n" +
                "VAT can not be negative")
        @DecimalMax(value = "100.0", message = "\n" +
                "VAT cannot exceed 100%")
        BigDecimal taxRate,

        @NotNull(message = "\n" +
                "Please enter the exchange rate.")
        @DecimalMin(value = "0.01", message = "\n" +
                "The exchange rate must be greater than 0.")
        @DecimalMax(value = "100000", message = "Exchange rate is too high")
        BigDecimal usdToKhr,

        @NotNull(message = "\n" +
                "Please enter the lower limit.")
        @Min(value = 0, message = "Low inventory levels cannot be negative")
        Integer defaultLowStockThreshold
) {
}
