package com.sujay.ledgerflow.wallet;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Simulated external funding for the authenticated user's wallet. */
public record DepositRequest(
        @NotNull @DecimalMin(value = "0", inclusive = false, message = "must be greater than zero")
        @Digits(integer = 17, fraction = 2, message = "must contain at most 17 whole digits and 2 decimal places")
        @Schema(example = "1000.00", description = "Positive INR amount with no more than two decimal places") BigDecimal amount) {
}
