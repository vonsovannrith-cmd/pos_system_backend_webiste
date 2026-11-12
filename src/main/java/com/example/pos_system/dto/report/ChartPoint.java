package com.example.pos_system.dto.report;

import java.math.BigDecimal;

public record ChartPoint(

        String label,
        long orders,
        BigDecimal revenue
) {
}
