package com.example.pos_system.dto.response;

import com.example.pos_system.dto.report.ProductSales;
import com.example.pos_system.dto.report.SalesSummary;

import java.util.List;

public record DashboardResponse(

        SalesSummary today,
        SalesSummary month,
        long lowStockCount,
        List<ProductSales> topProducts
) {
}
