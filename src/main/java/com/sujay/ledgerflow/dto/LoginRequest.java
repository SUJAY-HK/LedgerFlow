package com.sujay.ledgerflow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "email is required") @Email(message = "email must be valid") @Size(max = 254) String email,
        @NotBlank(message = "password is required") @Size(max = 72) String password) {
}
