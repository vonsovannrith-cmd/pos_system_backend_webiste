
package com.example.pos_system;

import com.example.pos_system.common.BusinessException;
import com.example.pos_system.common.ResourceNotFoundException;
import com.example.pos_system.dto.request.CheckoutItemRequest;
import com.example.pos_system.dto.request.CheckoutRequest;
import com.example.pos_system.dto.response.OrderResponse;
import com.example.pos_system.model.entity.Order;
import com.example.pos_system.model.entity.Product;
import com.example.pos_system.model.entity.Settings;
import com.example.pos_system.model.entity.enums.DiscountType;
import com.example.pos_system.model.entity.enums.MovementType;
import com.example.pos_system.model.entity.enums.PaymentCurrency;
import com.example.pos_system.model.entity.enums.PaymentMethod;
import com.example.pos_system.model.repository.order.OrderRepository;
import com.example.pos_system.model.repository.product.ProductRepository;
import com.example.pos_system.model.service.inventory.InventoryService;
import com.example.pos_system.model.service.order.OrderService;
import com.example.pos_system.model.service.setting.SettingsService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final String CASHIER = "cashier@pos.com";

    @Mock
    OrderRepository orderRepository;

    @Mock
    ProductRepository productRepository;

    @Mock
    InventoryService inventoryService;

    @Mock
    SettingsService settingsService;

    @InjectMocks
    OrderService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                service,
                "cashierMaxDiscountPercent",
                new BigDecimal("10")
        );

        Settings settings = new Settings();
        settings.setTaxRate(new BigDecimal("10"));
        settings.setUsdToKhr(new BigDecimal("4100"));

        lenient().when(settingsService.current()).thenReturn(settings);
        lenient().when(orderRepository.nextOrderSeq()).thenReturn(1L);

        lenient().when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // =====================================================
    // Helpers
    // =====================================================

    private Product product(long id, String price, int stock, boolean active) {
        Product product = new Product();
        product.setId(id);
        product.setName("Product " + id);
        product.setSku("SKU-" + id);
        product.setPrice(new BigDecimal(price));
        product.setStockQuantity(stock);
        product.setActive(active);
        return product;
    }

    private void givenProduct(Product product) {
        when(productRepository.findWithLockById(product.getId()))
                .thenReturn(Optional.of(product));
    }

    private CheckoutItemRequest item(long productId, int quantity) {
        return new CheckoutItemRequest(productId, quantity);
    }

    private CheckoutRequest cash(
            String paid,
            DiscountType type,
            String discount,
            CheckoutItemRequest... items
    ) {
        return new CheckoutRequest(
                List.of(items),
                type,
                discount == null ? null : new BigDecimal(discount),
                PaymentMethod.CASH,
                new BigDecimal(paid),
                null
        );
    }

    private CheckoutRequest khr(
            String paid,
            CheckoutItemRequest... items
    ) {
        return new CheckoutRequest(
                List.of(items),
                null,
                null,
                PaymentMethod.CASH,
                new BigDecimal(paid),
                PaymentCurrency.KHR
        );
    }

    // =====================================================
    // KHR Payment Tests
    // =====================================================

    @Test
    void checkout_khrComputesDueAndChange() {
        givenProduct(product(1, "0.75", 48, true));

        OrderResponse response = service.checkout(
                khr("10000", item(1, 2)),
                CASHIER,
                false
        );

        assertThat(response.total()).isEqualByComparingTo("1.65");
        assertThat(response.totalKhr()).isEqualByComparingTo("6800");
        assertThat(response.exchangeRate()).isEqualByComparingTo("4100");
        assertThat(response.paymentCurrency()).isEqualTo("KHR");
        assertThat(response.amountPaid()).isEqualByComparingTo("10000");
        assertThat(response.changeAmount()).isEqualByComparingTo("3200");
    }

    @Test
    void checkout_khrRoundsToNearest100HalfUp() {
        givenProduct(product(1, "0.45", 48, true));

        OrderResponse response = service.checkout(
                khr("5000", item(1, 1)),
                CASHIER,
                false
        );

        assertThat(response.total()).isEqualByComparingTo("0.50");
        assertThat(response.totalKhr()).isEqualByComparingTo("2100");
        assertThat(response.changeAmount()).isEqualByComparingTo("2900");
    }

    @Test
    void checkout_khrRejectsInsufficientAmount() {
        givenProduct(product(1, "0.75", 48, true));

        assertThatThrownBy(() ->
                service.checkout(
                        khr("6700", item(1, 2)),
                        CASHIER,
                        false
                )
        )
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient amount of riel");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void checkout_khrIsOnlyAllowedForCash() {
        CheckoutRequest request = new CheckoutRequest(
                List.of(item(1, 1)),
                null,
                null,
                PaymentMethod.KHQR,
                null,
                PaymentCurrency.KHR
        );

        assertThatThrownBy(() ->
                service.checkout(request, CASHIER, false)
        )
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Riel can only be used for cash payments");

        verify(orderRepository, never()).save(any(Order.class));
    }

    // =====================================================
    // Discount Tests
    // =====================================================

    @Test
    void checkout_cashierCannotExceedDiscountLimit() {
        givenProduct(product(1, "0.75", 48, true));

        CheckoutRequest request = cash(
                "5",
                DiscountType.PERCENT,
                "20",
                item(1, 2)
        );

        assertThatThrownBy(() ->
                service.checkout(request, CASHIER, false)
        )
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Maximum discount a cashier can apply");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void checkout_privilegedUserMayGiveLargerDiscount() {
        givenProduct(product(1, "0.75", 48, true));

        OrderResponse response = service.checkout(
                cash("5", DiscountType.PERCENT, "20", item(1, 2)),
                "manager@pos.com",
                true
        );

        assertThat(response.discountAmount()).isEqualByComparingTo("0.30");
        assertThat(response.total()).isEqualByComparingTo("1.32");
    }

    // =====================================================
    // Cash Payment Tests
    // =====================================================

    @Test
    void checkout_rejectsInsufficientCash() {
        givenProduct(product(1, "0.75", 48, true));

        assertThatThrownBy(() ->
                service.checkout(
                        cash("1", null, null, item(1, 2)),
                        CASHIER,
                        false
                )
        )
                .isInstanceOf(BusinessException.class);

        verify(orderRepository, never()).save(any(Order.class));
    }

    // =====================================================
    // Inventory Tests
    // =====================================================

    @Test
    void checkout_doesNotSaveWhenStockIsInsufficient() {
        Product product = product(1, "0.75", 1, true);
        givenProduct(product);

        doThrow(new BusinessException(
                "Insufficient stock",
                HttpStatus.BAD_REQUEST
        ))
                .when(inventoryService)
                .applyMovement(
                        any(),
                        anyInt(),
                        any(),
                        anyString(),
                        anyString()
                );

        assertThatThrownBy(() ->
                service.checkout(
                        cash("50", null, null, item(1, 5)),
                        CASHIER,
                        false
                )
        )
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("stock");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void checkout_mergesDuplicateItems() {
        Product cola = product(1, "1.00", 48, true);
        givenProduct(cola);

        OrderResponse response = service.checkout(
                cash(
                        "10",
                        null,
                        null,
                        item(1, 1),
                        item(1, 2)
                ),
                CASHIER,
                false
        );

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quantity()).isEqualTo(3);

        verify(inventoryService).applyMovement(
                eq(cola),
                eq(-3),
                eq(MovementType.SALE),
                anyString(),
                eq(CASHIER)
        );
    }

    @Test
    void checkout_rejectsInactiveProduct() {
        givenProduct(product(1, "0.75", 48, false));

        assertThatThrownBy(() ->
                service.checkout(
                        cash("5", null, null, item(1, 1)),
                        CASHIER,
                        false
                )
        )
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Sales closed");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void checkout_rejectsUnknownProduct() {
        when(productRepository.findWithLockById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.checkout(
                        cash("5", null, null, item(99, 1)),
                        CASHIER,
                        false
                )
        )
                .isInstanceOf(ResourceNotFoundException.class);

        verify(orderRepository, never()).save(any(Order.class));
    }
}
