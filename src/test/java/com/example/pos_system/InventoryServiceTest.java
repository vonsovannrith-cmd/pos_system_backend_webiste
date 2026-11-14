package com.example.pos_system;

import com.example.pos_system.common.BusinessException;
import com.example.pos_system.model.entity.Product;
import com.example.pos_system.model.entity.StockMovement;
import com.example.pos_system.model.entity.enums.MovementType;
import com.example.pos_system.model.repository.inventory.StockMovementRepository;
import com.example.pos_system.model.repository.product.ProductRepository;
import com.example.pos_system.model.service.inventory.InventoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    ProductRepository productRepository;
    @Mock
    StockMovementRepository movementRepository;
    @InjectMocks
    InventoryService service;

    private Product productWithStock(int stock) {
        Product p = new Product();
        p.setName("Cola");
        p.setStockQuantity(stock);
        return p;
    }

    @Test
    void applyMovement_rejectsNegativeStockAndChangesNothing() {
        Product p = productWithStock(1);

        assertThatThrownBy(() -> service.applyMovement(p, -2, MovementType.SALE, "test", "a@pos.com"))
                .isInstanceOf(BusinessException.class);

        assertThat(p.getStockQuantity()).isEqualTo(1);
        verifyNoInteractions(movementRepository);
    }

    @Test
    void applyMovement_updatesStockAndRecordsMovement() {
        Product p = productWithStock(10);
        when(movementRepository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        StockMovement m = service.applyMovement(p, 5, MovementType.STOCK_IN, "delivery", "a@pos.com");

        assertThat(p.getStockQuantity()).isEqualTo(15);
        assertThat(m.getQuantity()).isEqualTo(5);
        assertThat(m.getStockAfter()).isEqualTo(15);
        assertThat(m.getCreatedBy()).isEqualTo("a@pos.com");
    }
}
