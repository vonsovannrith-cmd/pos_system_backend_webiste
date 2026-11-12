package com.example.pos_system.dto.response;

public record HeldItemResponse(

        ProductResponse product,
        int quantity
) {
}
