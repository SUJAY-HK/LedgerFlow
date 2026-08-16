package com.sujay.ledgerflow.mapper;

import com.sujay.ledgerflow.wallet.Wallet;
import com.sujay.ledgerflow.wallet.WalletBalanceResponse;
import com.sujay.ledgerflow.wallet.WalletResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class WalletMapper {

    public WalletResponse toResponse(Wallet wallet) {
        return new WalletResponse(wallet.getId(), balance(wallet), wallet.getCurrency(), wallet.getStatus());
    }

    public WalletBalanceResponse toBalanceResponse(Wallet wallet) {
        return new WalletBalanceResponse(balance(wallet), wallet.getCurrency());
    }

    private BigDecimal balance(Wallet wallet) {
        // The entity's NUMERIC(19,2) contract guarantees this is an exact paise scale, never rounded here.
        return wallet.getBalance().setScale(2, RoundingMode.UNNECESSARY);
    }
}
