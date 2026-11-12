package com.example.pos_system.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest (

        @NotBlank(message = "Please enter the Email")
        @Email(message = "Incorrect of Email")
        String email,

        @NotBlank(message = "Please enter the Password")
        String password
){
}
