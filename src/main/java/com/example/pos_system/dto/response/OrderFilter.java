package com.example.pos_system.dto.response;

import com.example.pos_system.model.entity.enums.OrderStatus;
import com.example.pos_system.model.entity.enums.PaymentMethod;

import java.time.LocalDate;

public record OrderFilter(

        String keyword,
        LocalDate from,
        LocalDate to,
        String cashierEmail,
        PaymentMethod paymentMethod,
        OrderStatus status
) {
}
