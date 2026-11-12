package com.example.pos_system.model.repository.order;

import com.example.pos_system.dto.report.CashierSales;
import com.example.pos_system.dto.report.SalesSummary;
import com.example.pos_system.model.entity.Order;
import com.example.pos_system.model.entity.enums.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    // Role: Use a PostgreSQL database sequence (`order_number_seq`) to retrieve a unique sequential number for generating an `orderNumber` (e.g., ORD-20261105-0001).
    @Query(value = "SELECT nextval('order_number_seq')", nativeQuery = true)
    Long nextOrderSeq();

    @EntityGraph(attributePaths = "items")
    Optional<Order> findWithItemsById(Long id);

    // lock only order
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findWithLockById(Long id);

    @Query("""
        SELECT new com.example.pos_system.dto.report.SalesSummary(
            COUNT(o), SUM(o.total), SUM(o.discountAmount), SUM(o.taxAmount))
        FROM Order o
        WHERE o.status = :status AND o.createdAt >= :start AND o.createdAt < :end
        """)
    SalesSummary summarize(@Param("status") OrderStatus status,
                           @Param("start") LocalDateTime start,
                           @Param("end") LocalDateTime end);

    @Query("""
        SELECT new com.example.pos_system.dto.report.CashierSales(
            o.cashierEmail, COUNT(o), SUM(o.total))
        FROM Order o
        WHERE o.status = :status AND o.createdAt >= :start AND o.createdAt < :end
        GROUP BY o.cashierEmail
        ORDER BY SUM(o.total) DESC
        """)
    List<CashierSales> salesByCashier(@Param("status") OrderStatus status,
                                      @Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);

    // True SQL: [0]=Day, [1]=Order Quantity, [2]=Revenue
    @Query(value = """
        SELECT CAST(o.created_at AS date) AS day, COUNT(*) AS orders, COALESCE(SUM(o.total), 0) AS revenue
        FROM orders o
        WHERE o.status = 'COMPLETED' AND o.created_at >= :start AND o.created_at < :end
        GROUP BY CAST(o.created_at AS date)
        ORDER BY 1
        """, nativeQuery = true)
    List<Object[]> dailySales(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

}
