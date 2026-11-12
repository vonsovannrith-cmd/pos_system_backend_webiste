package com.example.pos_system.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "settings")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Settings {

    public static final long ID = 1L;

    @Id
    private Long id = ID;

    @Column(name = "store_name", nullable = false, length = 100)
    private String storeName;

    @Column(length = 255)
    private String address;

    @Column(length = 30)
    private String phone;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate;

    @Column(name = "usd_to_khr", nullable = false, precision = 10, scale = 2)
    private BigDecimal usdToKhr;

    @Column(name = "default_low_stock", nullable = false)
    private int defaultLowStock;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "updated_by", length = 150)
    private String updatedBy;
}
