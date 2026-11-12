package com.example.pos_system.model.repository.product;

import com.example.pos_system.model.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;

import java.util.List;
import java.util.Optional;

public interface ProductRepository
        extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> { // A key feature! It allows you to build dynamic queries—for example, searching for products by name, category, or price range—while also supporting pagination.

    boolean existsBySku(String sku);
    boolean existsBySkuAndIdNot(String sku, Long id);
    boolean existsByBarcode(String barcode);
    boolean existsByBarcodeAndIdNot(String barcode, Long id);
    boolean existsByCategoryId(Long categoryId);

    // + issue: The N+1 Problem: An issue in JPA where, if you retrieve 10 products, the system performs 10 additional queries to the database to fetch their associated categories (totaling 11 queries), which slows down the system.
    // + Solution:  (@EntityGraph): This annotation instructs Spring Data JPA to fetch both the Product and its Category together in a single SQL query (using a JOIN) when performing pagination and filtering—even if the relationship is defined as LAZY. This significantly boosts performance!
    // Fetch the Category within the same query (avoiding N+1)
    @Override
    @EntityGraph(attributePaths = "category")
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    // + issue : Suppose two customers are purchasing the same item at the same time (two cashiers are recording the sales simultaneously).
    // Lock database rows when updating stock
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Product> findWithLockById(Long id);

    // Used to display a "Low Stock Alert" on the POS dashboard, indicating which items are running low in stock.
    @Query("""
            SELECT p FROM Product p
            LEFT JOIN FETCH p.category
            WHERE p.active = true AND p.stockQuantity <= p.lowStockThreshold
            ORDER BY p.stockQuantity ASC
            """)
    List<Product> findLowStock();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.active = true AND p.stockQuantity <= p.lowStockThreshold")
    long countLowStock();
}
