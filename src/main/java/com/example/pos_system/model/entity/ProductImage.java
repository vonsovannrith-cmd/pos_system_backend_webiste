package com.example.pos_system.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "product_images")
@Getter
@Setter
@NoArgsConstructor

public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(nullable = false)
    private byte[] data;          // ⚠️ Do not use @Lob with PostgreSQL (it becomes an OID)

    @Column(name = "size_bytes", nullable = false)
    private int sizeBytes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ProductImage(String contentType, byte[] data) {
        this.contentType = contentType;
        this.data = data;
        this.sizeBytes = data.length;
    }
}
