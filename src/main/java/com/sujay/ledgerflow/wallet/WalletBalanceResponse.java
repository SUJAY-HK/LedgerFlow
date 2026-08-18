package com.sujay.ledgerflow.wallet;

import java.math.BigDecimal;

/** Narrow response for clients that only need the current balance and currency. */
public record WalletBalanceResponse(BigDecimal balance, WalletCurrency currency) {
}
