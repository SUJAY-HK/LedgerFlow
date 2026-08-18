package com.sujay.ledgerflow.wallet;

import java.math.BigDecimal;
import java.util.UUID;

/** Safe, stable representation of the authenticated user's wallet. */
public record WalletResponse(UUID walletId, BigDecimal balance, WalletCurrency currency, WalletStatus status) {
}
