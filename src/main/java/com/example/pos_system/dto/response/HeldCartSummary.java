package com.example.pos_system.dto.response;

import java.time.LocalDateTime;

public record HeldCartSummary(

        Long id,
        String label,
        int itemCount,
        int totalQuantity,
        LocalDateTime createdAt
) {
}
