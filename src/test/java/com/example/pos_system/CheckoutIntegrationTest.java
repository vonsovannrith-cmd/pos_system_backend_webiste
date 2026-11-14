package com.example.pos_system;

import com.example.pos_system.common.BusinessException;
import com.example.pos_system.dto.request.CheckoutItemRequest;
import com.example.pos_system.dto.request.CheckoutRequest;
import com.example.pos_system.dto.request.ProductRequest;
import com.example.pos_system.dto.request.StockInRequest;
import com.example.pos_system.dto.response.ProductResponse;
import com.example.pos_system.model.entity.StockMovement;
import com.example.pos_system.model.entity.enums.PaymentMethod;
import com.example.pos_system.model.repository.inventory.StockMovementRepository;
import com.example.pos_system.model.repository.order.OrderRepository;
import com.example.pos_system.model.repository.product.ProductRepository;
import com.example.pos_system.model.service.inventory.InventoryService;
import com.example.pos_system.model.service.order.OrderService;
import com.example.pos_system.model.service.product.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckoutIntegrationTest extends IntegrationTestBase {

    private static final String CASHIER = "cashier@pos.com";

    @Autowired
    OrderService orderService;
    @Autowired
    ProductService productService;
    @Autowired
    InventoryService inventoryService;
    @Autowired
    ProductRepository productRepository;
    @Autowired
    OrderRepository orderRepository;
    @Autowired
    StockMovementRepository movementRepository;

    // ---------- helpers ----------

    private long newProduct(String price, int stock) {
        String sku = "IT-" + UUID.randomUUID().toString().substring(0, 8);
        ProductResponse p = productService.create(new ProductRequest(
                "IT " + sku, sku, null, new BigDecimal(price), BigDecimal.ZERO, 1, null, true));
        if (stock > 0) {
            inventoryService.stockIn(p.id(), new StockInRequest(stock, "seed"), "admin@pos.com");
        }
        return p.id();
    }

    private CheckoutRequest cash(CheckoutItemRequest... items) {
        return new CheckoutRequest(List.of(items), null, null,
                PaymentMethod.CASH, new BigDecimal("1000"), null);
    }

    private int stockOf(long productId) {
        return productRepository.findById(productId).orElseThrow().getStockQuantity();
    }

    // ---------- tests ----------

    @Test
    void concurrentCheckoutsNeverOversell() throws Exception {
        long id = newProduct("0.75", 3);        // ស្តុកត្រឹម 3
        int threads = 8;                         // តែមាន Cashier 8 នាក់ព្រមគ្នា

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                ready.countDown();
                go.await();                      // ចាប់ផ្តើមព្រមគ្នា
                try {
                    orderService.checkout(cash(new CheckoutItemRequest(id, 1)), CASHIER, false);
                    return true;
                } catch (BusinessException e) {  // ស្តុកមិនគ្រប់ = ជាការរំពឹងទុក
                    return false;
                }
            }));
        }
        ready.await();
        go.countDown();

        long sold = 0;
        for (Future<Boolean> f : results) {
            if (f.get(60, TimeUnit.SECONDS)) sold++;
        }
        pool.shutdown();

        assertThat(sold).isEqualTo(3);           // លក់បានតែ 3 ដូចស្តុក
        assertThat(stockOf(id)).isZero();        // មិនអវិជ្ជមាន

        List<StockMovement> movements =
                movementRepository.findByProductId(id, Pageable.unpaged()).getContent();
        assertThat(movements).hasSize(4);        // 1 STOCK_IN + 3 SALE
        assertThat(movements).allSatisfy(m -> assertThat(m.getStockAfter()).isGreaterThanOrEqualTo(0));
        assertThat(movements.stream().mapToInt(StockMovement::getQuantity).sum()).isZero();
    }

    @Test
    void failedCheckoutRollsBackStockAndOrder() {
        long a = newProduct("1.00", 5);          // id តូចជាង → ត្រូវកាត់មុន
        long b = newProduct("1.00", 0);          // អស់ស្តុក → បរាជ័យ
        long ordersBefore = orderRepository.count();

        assertThatThrownBy(() -> orderService.checkout(
                cash(new CheckoutItemRequest(a, 1), new CheckoutItemRequest(b, 1)), CASHIER, false))
                .isInstanceOf(BusinessException.class);

        assertThat(stockOf(a)).isEqualTo(5);                       // ត្រូវបាន Rollback ✅
        assertThat(orderRepository.count()).isEqualTo(ordersBefore);
        assertThat(movementRepository.findByProductId(a, Pageable.unpaged()).getContent())
                .hasSize(1);                                       // មានតែ STOCK_IN (គ្មាន SALE)
    }
}
