package com.example.pos_system;

import com.example.pos_system.common.BusinessException;
import com.example.pos_system.common.ResourceNotFoundException;
import com.example.pos_system.dto.request.CheckoutItemRequest;
import com.example.pos_system.dto.request.HoldCartRequest;
import com.example.pos_system.dto.response.HeldCartSummary;
import com.example.pos_system.model.entity.HeldCart;
import com.example.pos_system.model.entity.Product;
import com.example.pos_system.model.repository.HeldCartRepository;
import com.example.pos_system.model.repository.product.ProductRepository;
import com.example.pos_system.model.service.HeldCartService;
import com.example.pos_system.model.service.product.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeldCartServiceTest {

    private static final String CASHIER = "cashier@pos.com";

    @Mock
    HeldCartRepository heldCartRepository;
    @Mock
    ProductRepository productRepository;
    @Mock
    ProductService productService;
    @InjectMocks
    HeldCartService service;

    private Product product(long id) {
        Product p = new Product();
        p.setId(id);
        return p;
    }

    @Test
    void hold_mergesDuplicateItems() {
        when(productRepository.findAllById(any())).thenReturn(List.of(product(1)));
        when(heldCartRepository.save(any(HeldCart.class))).thenAnswer(inv -> inv.getArgument(0));

        HoldCartRequest req = new HoldCartRequest("  Red shirt  ",
                List.of(new CheckoutItemRequest(1L, 1), new CheckoutItemRequest(1L, 2)));

        HeldCartSummary summary = service.hold(req, CASHIER);

        assertThat(summary.itemCount()).isEqualTo(1);
        assertThat(summary.totalQuantity()).isEqualTo(3);
        assertThat(summary.label()).isEqualTo("Red shirt");   // trim
    }

    @Test
    void hold_rejectsWhenLimitIsReached() {
        when(heldCartRepository.countByCashierEmail(CASHIER)).thenReturn(20L);

        HoldCartRequest req = new HoldCartRequest(null, List.of(new CheckoutItemRequest(1L, 1)));

        assertThatThrownBy(() -> service.hold(req, CASHIER))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("too many suspended baskets");
    }

    @Test
    void hold_rejectsUnknownProduct() {
        when(productRepository.findAllById(any())).thenReturn(List.of());

        HoldCartRequest req = new HoldCartRequest(null, List.of(new CheckoutItemRequest(99L, 1)));

        assertThatThrownBy(() -> service.hold(req, CASHIER))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void resume_otherCashiersCartIsNotFound() {
        when(heldCartRepository.findWithLockByIdAndCashierEmail(5L, "other@pos.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resume(5L, "other@pos.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}