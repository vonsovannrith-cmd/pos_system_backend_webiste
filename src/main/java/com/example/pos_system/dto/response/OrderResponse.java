package com.example.pos_system.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(

        Long id,
        String orderNumber,
        String cashierEmail,
        List<OrderItemResponse> items,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal taxRate,
        BigDecimal taxAmount,
        BigDecimal total,
        String paymentMethod,
        BigDecimal amountPaid,
        BigDecimal changeAmount,
        String status,
        LocalDateTime createdAt,
        String cancelReason,
        String cancelledBy,
        LocalDateTime cancelledAt,
        String paymentCurrency,     // new
        BigDecimal exchangeRate,    // New (can be null for old orders)
        BigDecimal totalKhr         // New (can be null for old orders)
) {}
