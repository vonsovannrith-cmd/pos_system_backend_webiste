package com.example.pos_system.model.service.order;

import com.example.pos_system.common.BusinessException;
import com.example.pos_system.common.ResourceNotFoundException;
import com.example.pos_system.dto.request.CheckoutItemRequest;
import com.example.pos_system.dto.request.CheckoutRequest;
import com.example.pos_system.dto.response.*;
import com.example.pos_system.model.entity.Order;
import com.example.pos_system.model.entity.OrderItem;
import com.example.pos_system.model.entity.Product;
import com.example.pos_system.model.entity.Settings;
import com.example.pos_system.model.entity.enums.*;
import com.example.pos_system.model.repository.order.OrderRepository;
import com.example.pos_system.model.repository.product.ProductRepository;
import com.example.pos_system.model.service.inventory.InventoryService;
import com.example.pos_system.model.service.setting.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Pageable;
import com.example.pos_system.dto.response.PageResponse;
import org.springframework.http.HttpStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final SettingsService settingsService;

//    @Value("${app.pos.tax-rate}")
//    private BigDecimal taxRate;
    @Value("${app.pos.cashier-max-discount-percent}")
    private BigDecimal cashierMaxDiscountPercent;

    // check out method
    @Transactional
    public OrderResponse checkout(CheckoutRequest request, String email, boolean privileged) {

        //TreeMap (Hierarchical Treemap)
        Map<Long, Integer> quantities = new TreeMap<>();
//        BigDecimal taxRate = settingsService.current().getTaxRate();
        Settings settings = settingsService.current();
        BigDecimal taxRate = settings.getTaxRate();
        BigDecimal rate = settings.getUsdToKhr();

        PaymentCurrency currency = request.paymentCurrency() != null ? request.paymentCurrency() : PaymentCurrency.USD;
        if (currency == PaymentCurrency.KHR && request.paymentMethod() != PaymentMethod.CASH) {
            throw new BusinessException("The Riel can only be used for cash payments.", HttpStatus.BAD_REQUEST);
        }
        for (CheckoutItemRequest item : request.items()) {
            quantities.merge(item.productId(), item.quantity(), Integer::sum);
        }

        // create object order
        Order order = new Order();
        order.setOrderNumber(nextOrderNumber());
        order.setCashierEmail(email);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Product product = productRepository.findWithLockById(entry.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product ID not found " + entry.getKey()
                    ));
            if (!product.isActive()) {
                throw new BusinessException(product.getName() + " Sales closed", HttpStatus.BAD_REQUEST);
            }
            int qty = entry.getValue();
            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(qty));

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());    // Snapshot
            orderItem.setSku(product.getSku());
            orderItem.setUnitPrice(product.getPrice());     // Value from Database
            orderItem.setQuantity(qty);
            orderItem.setLineTotal(lineTotal);
            order.addItem(orderItem);

            // Business Exception if stock is not enough -> Transaction -> All Rollback
            inventoryService.applyMovement(product, -qty, MovementType.SALE,
                    "Order " + order.getOrderNumber(), email);
            subtotal = subtotal.add(lineTotal);
        }
        // 3. Discount → Tax → Total
        BigDecimal discount = calcDiscount(subtotal, request.discountType(), request.discountValue());

        if (!privileged) {
            BigDecimal maxDiscount = subtotal.multiply(cashierMaxDiscountPercent)
                    .divide((BigDecimal.valueOf(100)), 2, RoundingMode.HALF_UP);
            if (discount.compareTo(maxDiscount) > 0) {
                throw new BusinessException("Maximum discount a cashier can apply "
                        + cashierMaxDiscountPercent.stripTrailingZeros().toPlainString()
                        + "% . Please call the Manager or Admin.", HttpStatus.BAD_REQUEST);
            }
        }

        BigDecimal taxable = subtotal.subtract(discount);
        BigDecimal tax = taxable.multiply(taxRate).divide((BigDecimal.valueOf(100)), 2, RoundingMode.HALF_UP);
        BigDecimal total = taxable.add(tax);
        BigDecimal totalKhr = toKhr(total, rate);   // In Riel, rounded to the nearest 100៛

        // 4. Payment Method
        BigDecimal paid;
        BigDecimal change;
        if (request.paymentMethod() == PaymentMethod.CARD) {
            if (request.amountPaid() == null || request.amountPaid().compareTo(total) < 0) {
                throw new BusinessException("The amount is not enough to payment", HttpStatus.BAD_REQUEST);
            }
            paid = request.amountPaid().setScale(2, RoundingMode.HALF_UP);
            change = paid.subtract(total);
        } else {
            paid = total;                           // KHQR/Card: Pay the exact amount (USD)
            change = BigDecimal.ZERO.setScale(2);
        }

        // Save in database
        order.setSubtotal(subtotal);
        order.setDiscountAmount(discount);
        order.setTaxRate(taxRate);
        order.setTaxAmount(tax);
        order.setTotal(total);
        order.setPaymentMethod(request.paymentMethod());
        order.setAmountPaid(paid);
        order.setChangeAmount(change);
        order.setPaymentCurrency(currency);
        order.setExchangeRate(rate);
        order.setTotalKhr(totalKhr);

        return toResponse(orderRepository.save(order));
    }

    //  Get invoice by ID
    @Transactional(readOnly = true)
    public OrderResponse get(Long id, String email, boolean privileged) {
        Order order = orderRepository.findWithItemsById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invoice not found"
                ));
        // Cashier only view her invoice
        if (!privileged && !order.getCashierEmail().equals(email)) {
            throw new BusinessException("Invoice not found", HttpStatus.BAD_REQUEST);
        }
        return toResponse(order);
    }

    // search history invoice
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> search(OrderFilter filter, Pageable pageable) {
        if (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to())) {
            throw new BusinessException("The start date must not be later than the end date.", HttpStatus.BAD_REQUEST);
        }
        return PageResponse.from(
                orderRepository.findAll(buildSpec(filter), pageable).map(this::toSummary));
    }

    // cancel method
    @Transactional
    public OrderResponse cancel(Long id, OrderStatus target, String reason, String email) {

        // 1) Lock the order. Those who come later must wait.
        Order order = orderRepository.findWithLockById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        // 2) Check status (prevent duplicate Void/Refund)
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new BusinessException(
                    "This invoice " + order.getStatus() + " Already", HttpStatus.CONFLICT);
        }
        if (target == OrderStatus.VOIDED
                && !order.getCreatedAt().toLocalDate().equals(LocalDate.now())) {
            throw new BusinessException("Voiding is only permitted on the day of the sale; please use the refund process instead.", HttpStatus.BAD_REQUEST);
        }

        // 3) Stock Allocation — Sorted by productId (locking in the same order as Checkout)
        List<OrderItem> items = order.getItems().stream()
                .sorted(Comparator.comparing(OrderItem::getProductId))
                .toList();

        for (OrderItem item : items) {
            Product product = productRepository.findWithLockById(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Can't find product in Invoice"));
            inventoryService.applyMovement(product, item.getQuantity(), MovementType.REFUND,
                    target.name() + " " + order.getOrderNumber(), email);
        }

        // 4) Change status + Record
        order.setStatus(target);
        order.setCancelReason(reason.trim());
        order.setCancelledBy(email);
        order.setCancelledAt(LocalDateTime.now());

        return toResponse(order);       // No need for save() — Dirty checking
    }

    // ---------- helpers ----------

    // calculate for discount
    private BigDecimal calcDiscount(BigDecimal subtotal, DiscountType type, BigDecimal value) {
        if (type == null || value == null || value.signum() == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal discount;
        if (type == DiscountType.PERCENT) {
            if (value.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new BusinessException("Discount can't more 100%", HttpStatus.BAD_REQUEST);
            }
            discount = subtotal.multiply(value).divide((BigDecimal.valueOf(100)), 2, RoundingMode.HALF_UP);
        } else {
            discount = value.setScale(2, RoundingMode.HALF_UP);
        }
        if (discount.compareTo(subtotal) > 0) {
            throw new BusinessException("The discount more than subtotal", HttpStatus.BAD_REQUEST);
        }
        return discount;
    }

    // used for dynamic search
    private Specification<Order> buildSpec(OrderFilter f) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();

            if (f.keyword() != null && !f.keyword().isBlank()) {
                ps.add(cb.like(cb.lower(root.get("orderNumber")),
                        "%" + f.keyword().trim().toLowerCase() + "%"));
            }
            if (f.from() != null) {
                ps.add(cb.greaterThanOrEqualTo(
                        root.<LocalDateTime>get("createdAt"), f.from().atStartOfDay()));
            }
            if (f.to() != null) {

                // < The following day at 00:00 = Includes the entire "to" day
                ps.add(cb.lessThan(
                        root.<LocalDateTime>get("createdAt"), f.to().plusDays(1).atStartOfDay()));
            }
            if (f.cashierEmail() != null && !f.cashierEmail().isBlank()) {
                ps.add(cb.equal(root.get("cashierEmail"), f.cashierEmail().trim()));
            }
            if (f.paymentMethod() != null) {
                ps.add(cb.equal(root.get("paymentMethod"), f.paymentMethod()));
            }
            if (f.status() != null) {
                ps.add(cb.equal(root.get("status"), f.status()));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    // convert entity order summary to dto for client
    private OrderSummaryResponse toSummary(Order o) {
        return new OrderSummaryResponse(
                o.getId(), o.getOrderNumber(), o.getCashierEmail(), o.getTotal(),
                o.getPaymentMethod().name(), o.getStatus().name(), o.getCreatedAt());
    }

    // generate order number of invoice
    private String nextOrderNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        return String.format("INV-%s-%06d", date, orderRepository.nextOrderSeq());
    }

    /** USD → Riel, rounded to the nearest 100៛ (HALF_UP) */
    private BigDecimal toKhr(BigDecimal usd, BigDecimal rate) {
        return usd.multiply(rate)
                .divide((BigDecimal.valueOf(100)), 0, RoundingMode.HALF_UP)
                .multiply((BigDecimal.valueOf(100)));
    }

    // convert entity to dto response to client
    private OrderResponse toResponse(Order o) {
        return new OrderResponse(
                o.getId(), o.getOrderNumber(), o.getCashierEmail(),
                o.getItems().stream()
                        .map(i -> new OrderItemResponse(i.getProductId(), i.getProductName(),
                                i.getSku(), i.getUnitPrice(), i.getQuantity(), i.getLineTotal()))
                        .toList(),
                o.getSubtotal(), o.getDiscountAmount(), o.getTaxRate(), o.getTaxAmount(),
                o.getTotal(), o.getPaymentMethod().name(), o.getAmountPaid(),
                o.getChangeAmount(), o.getStatus().name(), o.getCreatedAt(),
                o.getCancelReason(), o.getCancelledBy(), o.getCancelledAt(),
                o.getPaymentCurrency().name(), o.getExchangeRate(), o.getTotalKhr()
        );
    }
}
