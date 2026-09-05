package com.sujay.ledgerflow.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sujay.ledgerflow.exception.InsufficientBalanceException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Fast unit coverage for balance invariants owned directly by the Wallet domain object. */
class WalletTransferOperationsTest {

    @Test
    void debitAndCreditPreserveAnExactTwoDecimalBalance() {
        Wallet wallet = new Wallet(new BigDecimal("1000.00"));

        wallet.debit(new BigDecimal("500.00"));
        wallet.credit(new BigDecimal("125.50"));

        assertThat(wallet.getBalance()).isEqualByComparingTo("625.50");
        assertThat(wallet.getBalance().scale()).isEqualTo(2);
    }

    @Test
    void debitRejectsInsufficientBalanceWithoutChangingTheWallet() {
        Wallet wallet = new Wallet(new BigDecimal("100.00"));

        assertThatThrownBy(() -> wallet.debit(new BigDecimal("100.01"))).isInstanceOf(InsufficientBalanceException.class);

        assertThat(wallet.getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void balanceOperationsRejectZeroNegativeAndUnexpectedPrecision() {
        Wallet wallet = new Wallet(BigDecimal.ZERO);

        assertThatThrownBy(() -> wallet.credit(BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> wallet.credit(new BigDecimal("-1.00"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> wallet.credit(new BigDecimal("1.123"))).isInstanceOf(IllegalArgumentException.class);
        assertThat(wallet.getBalance()).isEqualByComparingTo("0.00");
    }
}
