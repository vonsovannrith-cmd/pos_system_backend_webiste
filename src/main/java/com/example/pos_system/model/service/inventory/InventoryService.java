package com.example.pos_system.model.service.inventory;

import com.example.pos_system.common.BusinessException;
import com.example.pos_system.common.ResourceNotFoundException;
import com.example.pos_system.dto.request.StockAdjustRequest;
import com.example.pos_system.dto.request.StockInRequest;
import com.example.pos_system.dto.response.PageResponse;
import com.example.pos_system.dto.response.StockMovementResponse;
import com.example.pos_system.model.entity.Product;
import com.example.pos_system.model.entity.StockMovement;
import com.example.pos_system.model.entity.enums.MovementType;
import com.example.pos_system.model.repository.product.ProductRepository;
import com.example.pos_system.model.repository.inventory.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;

    // stock in :
    @Transactional
    public StockMovementResponse stockIn(Long productId, StockInRequest request, String email) {
        Product product = lockProduct(productId);
        String reason = (request.reason() == null || request.reason().isBlank() ? "Stock in" : request.reason().trim());
        return toResponse(applyMovement(product, request.quantity(), MovementType.STOCK_IN, reason, email));
    }

    // adjust : use for change stock
    @Transactional
    public StockMovementResponse adjust(Long productId, StockAdjustRequest request, String email) {
        Product product = lockProduct(productId);
        int delta = request.newQuantity() - product.getStockQuantity();
        if (delta == 0) {
            throw new BusinessException("\n" +
                    "The stock quantity is as it is, no need to adjust.", HttpStatus.BAD_REQUEST);
        }
        return toResponse(applyMovement(product, delta, MovementType.ADJUSTMENT, request.reason().trim(), email));
    }

    // show history stock by product id and support pageable
    @Transactional(readOnly = true)
    public PageResponse<StockMovementResponse> movements(Long productId, Pageable pageable) {
        var page = (productId == null)
                ? stockMovementRepository.findAll(pageable)
                : stockMovementRepository.findByProductId(productId, pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    // calculate new stock and then reject if stock negative
    public StockMovement applyMovement(Product product, int delta, MovementType type, String reason, String email) {
        int newQuantity = product.getStockQuantity() + delta;
        if (newQuantity < 0) {
            throw new BusinessException(String.format(
                    "Insufficient stock: %s, %d remaining", product.getName(), product.getStockQuantity()
            ), HttpStatus.BAD_REQUEST);
        }
        product.setStockQuantity(newQuantity);

        return stockMovementRepository.save(StockMovement.builder()
                .product(product)
                .type(type)
                .quantity(delta)
                .stockAfter(newQuantity)
                .reason(reason)
                .createdBy(email)
                .createdAt(LocalDateTime.now())
                .build());
    }

    // To lock rows in the database, ensuring that no race conditions occur when simultaneously deducting or adding to stock.
    private Product lockProduct(Long id) {
        return productRepository.findWithLockById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product Id not Found"
                ));
    }

    // Convert the StockMovement entity to a StockMovementResponse DTO.
    private StockMovementResponse toResponse(StockMovement m) {
        return new StockMovementResponse(
                m.getId(),
                m.getProduct().getId(),
                m.getProduct().getName(),
                m.getType().name(),
                m.getQuantity(),
                m.getStockAfter(),
                m.getReason(),
                m.getCreatedBy(),
                m.getCreatedAt()
        );
    }
}
