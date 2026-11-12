package com.example.pos_system.dto.report;

import java.time.LocalDate;
import java.util.List;

public record SalesReport(

        LocalDate from,
        LocalDate to,
        SalesSummary summary,
        List<CashierSales> byCashier,
        List<ProductSales> byProduct,
        List<DailyPoint> daily
) {
}
