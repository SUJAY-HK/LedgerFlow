package com.sujay.ledgerflow.transaction;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(
        @NotNull @Schema(description = "UUID of the user receiving the funds") UUID recipientId,
        @NotNull @DecimalMin(value = "0", inclusive = false, message = "must be greater than zero")
        @Digits(integer = 17, fraction = 2, message = "must contain at most 17 whole digits and 2 decimal places")
        @Schema(example = "500.00", description = "Positive INR amount with no more than two decimal places") BigDecimal amount) {
}
