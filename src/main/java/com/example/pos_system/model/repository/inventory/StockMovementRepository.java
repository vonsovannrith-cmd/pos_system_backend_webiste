package com.example.pos_system.model.repository.inventory;

import com.example.pos_system.model.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    @Override
    @EntityGraph(attributePaths = "product")
    Page<StockMovement> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "product")
    Page<StockMovement> findByProductId(Long productId, Pageable pageable);
}
