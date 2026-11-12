package com.example.pos_system.dto.response;

import java.util.List;

public record HeldCartResponse(

        Long id,
        String label,
        List<HeldItemResponse> items
) {
}
