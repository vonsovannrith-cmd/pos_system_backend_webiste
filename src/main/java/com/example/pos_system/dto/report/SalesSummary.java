package com.example.pos_system.dto.report;

import java.math.BigDecimal;

public record SalesSummary(

        Long orderCount,
        BigDecimal revenue,
        BigDecimal discount,
        BigDecimal tax
) {
}
