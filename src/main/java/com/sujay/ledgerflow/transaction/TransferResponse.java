package com.sujay.ledgerflow.transaction;

import com.sujay.ledgerflow.wallet.WalletCurrency;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Stable API representation; wallet persistence relationships are deliberately not exposed. */
public record TransferResponse(
        UUID transactionId,
        UUID recipientId,
        BigDecimal amount,
        WalletCurrency currency,
        TransactionStatus status,
        Instant createdAt) {
}
