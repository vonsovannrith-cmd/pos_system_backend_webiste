package com.example.pos_system.dto.request;

import com.example.pos_system.model.entity.enums.DiscountType;
import com.example.pos_system.model.entity.enums.PaymentCurrency;
import com.example.pos_system.model.entity.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutRequest(

        @NotEmpty(message = "Empty basket")
        @Valid
        List<CheckoutItemRequest> items,

        DiscountType discountType,

        @DecimalMin(value = "0.0", message = "Discounts can not be negative")
        BigDecimal discountValue,

        @NotNull(message = "Please select a payment method.")
        PaymentMethod paymentMethod,

        @DecimalMin(value = "0.0", message = "Money received cannot be negative.")
        @DecimalMax(value = "999999999", message = "The amount of money is too large.")
        BigDecimal amountPaid,

        PaymentCurrency paymentCurrency // empty = USD
) {
}
