package com.example.pos_system.model.service.report;

import com.example.pos_system.common.BusinessException;
import com.example.pos_system.dto.report.ChartPoint;
import com.example.pos_system.dto.report.DailyPoint;
import com.example.pos_system.dto.report.SalesReport;
import com.example.pos_system.dto.report.SalesSummary;
import com.example.pos_system.dto.response.DashboardResponse;
import com.example.pos_system.model.entity.enums.ChartPeriod;
import com.example.pos_system.model.entity.enums.OrderStatus;
import com.example.pos_system.model.repository.order.OrderItemRepository;
import com.example.pos_system.model.repository.order.OrderRepository;
import com.example.pos_system.model.repository.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final OrderStatus DONE = OrderStatus.COMPLETED;
    private static final int MAX_RANGE_DAYS = 366;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    // ---------- Dashboard ----------

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        LocalDate today = LocalDate.now();
        return new DashboardResponse(
                summary(today, today),
                summary(today.withDayOfMonth(1), today),
                productRepository.countLowStock(),
                orderItemRepository.salesByProduct(
                        DONE, startOf(today.minusDays(6)), endOf(today), PageRequest.of(0, 5)));
    }

    // ---------- Chart ----------

    @Transactional(readOnly = true)
    public List<ChartPoint> chart(ChartPeriod period) {
        LocalDate today = LocalDate.now();
        LocalDate start = switch (period) {
            case DAY -> today.minusDays(29);                       // Last 30 days
            case WEEK -> today.with(DayOfWeek.MONDAY).minusWeeks(11);   // 12 week
            case MONTH -> today.withDayOfMonth(1).minusMonths(11);      // 12 month
        };

        // Create all empty buckets beforehand so that days/weeks/months with no sales are still displayed (with a value of 0)
        Map<LocalDate, Long> orders = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal> revenue = new LinkedHashMap<>();
        for (LocalDate d = start; !d.isAfter(today); d = next(d, period)) {
            orders.put(d, 0L);
            revenue.put(d, BigDecimal.ZERO);
        }

        for (DailyPoint p : daily(start, today)) {
            LocalDate key = bucket(p.date(), period);
            orders.merge(key, p.orders(), Long::sum);
            revenue.merge(key, p.revenue(), BigDecimal::add);
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(period == ChartPeriod.MONTH ? "yyyy-MM" : "MM-dd");
        return orders.keySet().stream()
                .map(k -> new ChartPoint(k.format(fmt), orders.get(k), revenue.get(k)))
                .toList();
    }

    // ---------- Sales report ----------

    @Transactional(readOnly = true)
    public SalesReport salesReport(LocalDate from, LocalDate to) {
        LocalDate end = (to != null) ? to : LocalDate.now();
        LocalDate start = (from != null) ? from : end.withDayOfMonth(1);

        if (start.isAfter(end)) {
            throw new BusinessException("The beginning is no greater than the end.", HttpStatus.BAD_REQUEST);
        }
        if (ChronoUnit.DAYS.between(start, end) > MAX_RANGE_DAYS) {
            throw new BusinessException("Date range too long (maximum " + MAX_RANGE_DAYS + "days)", HttpStatus.BAD_REQUEST);
        }

        return new SalesReport(
                start, end,
                summary(start, end),
                orderRepository.salesByCashier(DONE, startOf(start), endOf(end)),
                orderItemRepository.salesByProduct(DONE, startOf(start), endOf(end), Pageable.unpaged()),
                daily(start, end));
    }

    // ---------- helpers ----------

    private SalesSummary summary(LocalDate from, LocalDate to) {
        SalesSummary s = orderRepository.summarize(DONE, startOf(from), endOf(to));
        if (s == null) {
            return new SalesSummary(0L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        return new SalesSummary(
                s.orderCount() == null ? 0L : s.orderCount(),
                nz(s.revenue()), nz(s.discount()), nz(s.tax()));
    }

    private List<DailyPoint> daily(LocalDate from, LocalDate to) {
        return orderRepository.dailySales(startOf(from), endOf(to)).stream()
                .map(r -> new DailyPoint(toDate(r[0]), ((Number) r[1]).longValue(), toDecimal(r[2])))
                .toList();
    }

    private LocalDate bucket(LocalDate d, ChartPeriod period) {
        return switch (period) {
            case DAY -> d;
            case WEEK -> d.with(DayOfWeek.MONDAY);
            case MONTH -> d.withDayOfMonth(1);
        };
    }

    private LocalDate next(LocalDate d, ChartPeriod period) {
        return switch (period) {
            case DAY -> d.plusDays(1);
            case WEEK -> d.plusWeeks(1);
            case MONTH -> d.plusMonths(1);
        };
    }

    private LocalDateTime startOf(LocalDate d) {
        return d.atStartOfDay();
    }

    private LocalDateTime endOf(LocalDate d) {
        return d.plusDays(1).atStartOfDay();
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private BigDecimal toDecimal(Object v) {
        return v instanceof BigDecimal b ? b : new BigDecimal(v.toString());
    }

    private LocalDate toDate(Object v) {
        if (v instanceof java.sql.Date d) return d.toLocalDate();
        if (v instanceof LocalDate d) return d;
        if (v instanceof java.util.Date d) return new java.sql.Date(d.getTime()).toLocalDate();
        throw new IllegalStateException("Unexpected date type: " + v.getClass());
    }
}
