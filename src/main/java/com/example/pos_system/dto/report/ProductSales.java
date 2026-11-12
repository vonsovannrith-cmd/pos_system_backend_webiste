package com.example.pos_system.dto.report;

import java.math.BigDecimal;

public record ProductSales(

        Long productId,
        String productName,
        String sku,
        Long quantity,
        BigDecimal revenue
) {
}
