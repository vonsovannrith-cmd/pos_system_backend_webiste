package com.example.pos_system.dto.response;

import java.math.BigDecimal;

public record SettingsResponse(

        String storeName,
        String address,
        String phone,
        BigDecimal taxRate,
        BigDecimal usdToKhr,
        int defaultLowStockThreshold
) {
}
