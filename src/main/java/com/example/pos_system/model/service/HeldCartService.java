package com.example.pos_system.model.service;

import com.example.pos_system.common.BusinessException;
import com.example.pos_system.common.ResourceNotFoundException;
import com.example.pos_system.dto.response.HeldCartResponse;
import com.example.pos_system.dto.response.HeldCartSummary;
import com.example.pos_system.dto.response.HeldItemResponse;
import com.example.pos_system.dto.request.HoldCartRequest;
import com.example.pos_system.dto.request.CheckoutItemRequest;
import com.example.pos_system.model.entity.HeldCart;
import com.example.pos_system.model.entity.HeldItem;
import com.example.pos_system.model.entity.Product;
import com.example.pos_system.model.repository.HeldCartRepository;
import com.example.pos_system.model.repository.product.ProductRepository;
import com.example.pos_system.model.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HeldCartService {

    private static final int MAX_PER_CASHIER = 20;

    private final HeldCartRepository heldCartRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;

    // show list of held cart summary
    @Transactional(readOnly = true)
    public List<HeldCartSummary> list(String email) {
        return heldCartRepository.findByCashierEmailOrderByCreatedAtDesc(email).stream()
                .map(this::toSummary)
                .toList();
    }

    // hold cart
    @Transactional
    public HeldCartSummary hold(HoldCartRequest request, String email) {
        if (heldCartRepository.countByCashierEmail(email) >= MAX_PER_CASHIER) {
            throw new BusinessException("There are too many suspended baskets (maximum " + MAX_PER_CASHIER + ") " +
                    "Please resume or delete some items first.", HttpStatus.BAD_REQUEST);
        }

        // Consolidate duplicate items (like Checkout)
        Map<Long, Integer> quantities = new TreeMap<>();
        for (CheckoutItemRequest item : request.items()) {
            quantities.merge(item.productId(), item.quantity(), Integer::sum);
        }

        if (productRepository.findAllById(quantities.keySet()).size() != quantities.size()) {
            throw new ResourceNotFoundException("\n" +
                    "Some items could not be found.");
        }

        HeldCart cart = new HeldCart();
        cart.setCashierEmail(email);
        cart.setLabel((request.label() == null || request.label().isBlank()) ? null : request.label().trim());
        quantities.forEach((productId, qty) -> cart.getItems().add(new HeldItem(productId, qty)));

        return toSummary(heldCartRepository.save(cart));
    }

    /** Retrieve the basket and remove it from the suspended list, within a single transaction */
    @Transactional
    public HeldCartResponse resume(Long id, String email) {
        HeldCart cart = heldCartRepository.findWithLockByIdAndCashierEmail(id, email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Suspended basket not found."
                ));
        List<Long> ids = cart.getItems().stream().map(HeldItem::getProductId).toList();

        Map<Long, Product> products = productRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<HeldItemResponse> items = cart.getItems().stream()
                .map(i -> new HeldItemResponse(
                        productService.toResponse(products.get(i.getProductId())), i.getQuantity()
                )).toList();

        HeldCartResponse response = new HeldCartResponse(cart.getId(), cart.getLabel(), items);
        heldCartRepository.delete(cart);
        return response;
    }

    // delete cart
    @Transactional
    public void discard(Long id, String email) {
        HeldCart cart = heldCartRepository.findWithLockByIdAndCashierEmail(id, email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Suspended basket not found."
                ));
        heldCartRepository.delete(cart);
    }

    // convert entity to dto response to client
    private HeldCartSummary toSummary(HeldCart c) {
        int units = c.getItems().stream().mapToInt(HeldItem::getQuantity).sum();
        return new HeldCartSummary(
                c.getId(),
                c.getLabel(),
                c.getItems().size(),
                units,
                c.getCreatedAt()
        );
    }
}
