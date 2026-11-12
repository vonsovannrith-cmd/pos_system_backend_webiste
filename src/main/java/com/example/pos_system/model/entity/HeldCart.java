package com.example.pos_system.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "held_carts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HeldCart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cashier_email", nullable = false, length = 150)
    private String cashierEmail;

    @Column(length = 60)
    private String label;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ElementCollection
    @CollectionTable(name = "held_cart_items", joinColumns = @JoinColumn(name = "held_cart_id"))
    private List<HeldItem> items = new ArrayList<>();
}
