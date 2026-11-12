package com.example.pos_system.model.entity;

import com.example.pos_system.model.entity.enums.MovementType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MovementType type;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "stock_after", nullable = false)
    private int stockAfter;

    @Column(length = 255)
    private String reason;

    @Column(name = "created_by", nullable = false, length = 150)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
