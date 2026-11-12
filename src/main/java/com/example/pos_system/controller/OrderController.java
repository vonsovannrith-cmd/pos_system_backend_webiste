package com.example.pos_system.controller;

import com.example.pos_system.dto.request.CancelOrderRequest;
import com.example.pos_system.dto.request.CheckoutRequest;
import com.example.pos_system.dto.response.OrderFilter;
import com.example.pos_system.dto.response.OrderResponse;
import com.example.pos_system.dto.response.OrderSummaryResponse;
import com.example.pos_system.dto.response.PageResponse;
import com.example.pos_system.model.entity.enums.OrderStatus;
import com.example.pos_system.model.entity.enums.PaymentMethod;
import com.example.pos_system.model.service.order.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDate;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse checkout(@Valid @RequestBody CheckoutRequest request,
                                  Authentication authentication) {
        return orderService.checkout(request, authentication.getName(), isPrivileged(authentication));
    }

    @GetMapping
    public PageResponse<OrderSummaryResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String cashier,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {

        // A cashier is restricted to seeing only their own orders, even if `?cashier=...` is passed.
        String cashierEmail = isPrivileged(authentication) ? cashier : authentication.getName();

        return orderService.search(
                new OrderFilter(keyword, from, to, cashierEmail, paymentMethod, status), pageable);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id, Authentication authentication) {
        return orderService.get(id, authentication.getName(), isPrivileged(authentication));
    }

    @PostMapping("/{id}/void")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public OrderResponse voidOrder(@PathVariable Long id,
                                   @Valid @RequestBody CancelOrderRequest request,
                                   Authentication authentication) {
        return orderService.cancel(id, OrderStatus.VOIDED, request.reason(), authentication.getName());
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public OrderResponse refundOrder(@PathVariable Long id,
                                     @Valid @RequestBody CancelOrderRequest request,
                                     Authentication authentication) {
        return orderService.cancel(id, OrderStatus.REFUNDED, request.reason(), authentication.getName());
    }

    private boolean isPrivileged(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_MANAGER"));
    }
}
