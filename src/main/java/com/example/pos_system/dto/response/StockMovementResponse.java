package com.example.pos_system.dto.response;

import java.time.LocalDateTime;

public record StockMovementResponse(

        Long id,
        Long productId,
        String productName,
        String type,
        int quantity,
        int stockAfter,
        String reason,
        String createdBy,
        LocalDateTime createdAt
) {
}
