package com.example.pos_system.dto.report;

import java.math.BigDecimal;

public record CashierSales(

        String cashierEmail,
        Long orderCount,
        BigDecimal revenue
) {
}
