package com.sujay.ledgerflow.wallet;

import com.sujay.ledgerflow.transaction.TransactionStatus;
import com.sujay.ledgerflow.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** The completed simulated-funding record and resulting wallet balance. */
public record DepositResponse(
        UUID transactionId,
        TransactionType type,
        BigDecimal amount,
        WalletCurrency currency,
        TransactionStatus status,
        Instant createdAt,
        BigDecimal balance) {
}
