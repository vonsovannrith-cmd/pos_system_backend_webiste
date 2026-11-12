package com.example.pos_system.model.repository;

import com.example.pos_system.model.entity.HeldCart;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

public interface HeldCartRepository extends JpaRepository<HeldCart, Long> {

    @EntityGraph(attributePaths = "items")
    List<HeldCart> findByCashierEmailOrderByCreatedAtDesc(String cashierEmail);

    long countByCashierEmail(String cashierEmail);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<HeldCart> findWithLockByIdAndCashierEmail(Long id, String cashierEmail);
}
