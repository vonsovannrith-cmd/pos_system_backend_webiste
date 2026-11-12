package com.example.pos_system.controller;

import com.example.pos_system.dto.report.ChartPoint;
import com.example.pos_system.dto.report.SalesReport;
import com.example.pos_system.dto.response.DashboardResponse;
import com.example.pos_system.model.entity.enums.ChartPeriod;
import com.example.pos_system.model.service.report.ExcelExportService;
import com.example.pos_system.model.service.report.ReportService;
import org.springframework.http.MediaType;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class ReportController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ReportService reportService;
    private final ExcelExportService excelExportService;

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return reportService.dashboard();
    }

    @GetMapping("/sales-chart")
    public List<ChartPoint> chart(@RequestParam(defaultValue = "DAY") ChartPeriod period) {
        return reportService.chart(period);
    }

    @GetMapping("/sales")
    public SalesReport sales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.salesReport(from, to);
    }

    @GetMapping("/sales/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        SalesReport report = reportService.salesReport(from, to);
        byte[] bytes = excelExportService.salesReport(report);
        String filename = "sales-" + report.from() + "_" + report.to() + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(XLSX)
                .body(bytes);
    }
}
