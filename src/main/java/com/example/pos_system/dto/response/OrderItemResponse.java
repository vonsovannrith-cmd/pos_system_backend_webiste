package com.example.pos_system.dto.response;

import java.math.BigDecimal;

public record OrderItemResponse(

        Long productId,
        String productName,
        String sku,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal
) {
}
