package com.example.pos_system.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummaryResponse(

        Long id,
        String orderNumber,
        String cashierEmail,
        BigDecimal total,
        String paymentMethod,
        String status,
        LocalDateTime createdAt

) {
}
