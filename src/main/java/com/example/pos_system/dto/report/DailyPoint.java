package com.example.pos_system.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyPoint(

        LocalDate date,
        long orders,
        BigDecimal revenue

) {
}
