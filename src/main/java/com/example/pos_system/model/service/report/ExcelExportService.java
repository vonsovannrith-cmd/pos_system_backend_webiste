package com.example.pos_system.model.service.report;

import com.example.pos_system.dto.report.CashierSales;
import com.example.pos_system.dto.report.DailyPoint;
import com.example.pos_system.dto.report.ProductSales;
import com.example.pos_system.dto.report.SalesReport;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;

@Service
public class ExcelExportService {

    private record Styles(CellStyle head, CellStyle money) {}

    public byte[] salesReport(SalesReport r) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle head = wb.createCellStyle();
            Font bold = wb.createFont();
            bold.setBold(true);
            head.setFont(bold);

            CellStyle money = wb.createCellStyle();
            money.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));

            Styles st = new Styles(head, money);

            // --- Summary ---
            Sheet summary = wb.createSheet("Summary");
            write(summary, 0, st, true, "Item", "Value");
            write(summary, 1, st, false, "Period", r.from() + " to " + r.to());
            write(summary, 2, st, false, "Orders", r.summary().orderCount());
            write(summary, 3, st, false, "Revenue (incl. VAT)", r.summary().revenue());
            write(summary, 4, st, false, "Discount", r.summary().discount());
            write(summary, 5, st, false, "VAT", r.summary().tax());

            // --- By Cashier ---
            Sheet cashier = wb.createSheet("By Cashier");
            write(cashier, 0, st, true, "Cashier", "Orders", "Revenue");
            int i = 1;
            for (CashierSales c : r.byCashier()) {
                write(cashier, i++, st, false, c.cashierEmail(), c.orderCount(), c.revenue());
            }

            // --- By Product ---
            Sheet product = wb.createSheet("By Product");
            write(product, 0, st, true, "SKU", "Product", "Quantity", "Sales (before discount & VAT)");
            i = 1;
            for (ProductSales p : r.byProduct()) {
                write(product, i++, st, false, p.sku(), p.productName(), p.quantity(), p.revenue());
            }

            // --- Daily ---
            Sheet daily = wb.createSheet("Daily");
            write(daily, 0, st, true, "Date", "Orders", "Revenue");
            i = 1;
            for (DailyPoint d : r.daily()) {
                write(daily, i++, st, false, d.date().toString(), d.orders(), d.revenue());
            }

            for (Sheet s : wb) {
                for (int c = 0; c < 4; c++) {
                    s.setColumnWidth(c, 26 * 256);   // Set fixed width
                }
            }

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot build Excel file", e);
        }
    }

    private void write(Sheet sheet, int rowIdx, Styles st, boolean header, Object... values) {
        Row row = sheet.createRow(rowIdx);
        for (int c = 0; c < values.length; c++) {
            Cell cell = row.createCell(c);
            Object v = values[c];
            if (header) {
                cell.setCellValue(String.valueOf(v));
                cell.setCellStyle(st.head());
            } else if (v instanceof BigDecimal b) {
                cell.setCellValue(b.doubleValue());
                cell.setCellStyle(st.money());
            } else if (v instanceof Number n) {
                cell.setCellValue(n.doubleValue());
            } else {
                cell.setCellValue(String.valueOf(v));   // Text is not considered a Formula.
            }
        }
    }
}
