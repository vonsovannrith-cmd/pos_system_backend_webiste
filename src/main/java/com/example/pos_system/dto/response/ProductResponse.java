package com.example.pos_system.dto.response;

import java.math.BigDecimal;

public record ProductResponse(

        Long id,
        String name,
        String sku,
        String barcode,
        BigDecimal price,
        BigDecimal costPrice,
        int stockQuantity,
        int lowStockThreshold,
        boolean lowStock,
        boolean active,
        Long categoryId,
        String categoryName,
        String imageUrl
) {
}
