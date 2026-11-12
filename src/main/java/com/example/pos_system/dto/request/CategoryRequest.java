package com.example.pos_system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(

        @NotBlank(message = "Please enter the category name")
        @Size(max = 100, message = "Name too long (maximum 100)")
        String name,

        @Size(max = 255, message = "description too long")
        String description
) {}
