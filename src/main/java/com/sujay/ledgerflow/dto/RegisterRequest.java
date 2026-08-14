package com.sujay.ledgerflow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Public registration input; it is intentionally separate from the JPA User entity. */
public record RegisterRequest(
        @NotBlank(message = "name is required") @Size(max = 100) String name,
        @NotBlank(message = "email is required") @Email(message = "email must be valid") @Size(max = 254) String email,
        @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$", message = "phone must be a valid international phone number") String phone,
        @NotBlank(message = "password is required") @Size(min = 8, max = 72, message = "password must be between 8 and 72 characters") String password) {
}
