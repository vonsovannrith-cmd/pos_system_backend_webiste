package com.example.pos_system.model.repository.order;

import com.example.pos_system.dto.report.ProductSales;
import com.example.pos_system.model.entity.OrderItem;
import com.example.pos_system.model.entity.enums.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("""
            SELECT new com.example.pos_system.dto.report.ProductSales(
                i.productId, MAX(i.productName), MAX(i.sku), SUM(i.quantity), SUM(i.lineTotal))
            FROM OrderItem i
            WHERE i.order.status = :status
              AND i.order.createdAt >= :start AND i.order.createdAt < :end
            GROUP BY i.productId
            ORDER BY SUM(i.lineTotal) DESC, i.productId
            """)
    List<ProductSales> salesByProduct(@Param("status") OrderStatus status,
                                      @Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end,
                                      Pageable pageable);
}
