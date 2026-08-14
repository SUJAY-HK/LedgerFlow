package com.sujay.ledgerflow.auth;

import com.sujay.ledgerflow.repository.WalletRepository;
import com.sujay.ledgerflow.user.User;
import com.sujay.ledgerflow.wallet.Wallet;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

/** Creates the initial wallet as part of the registration business operation. */
@Service
public class WalletProvisioner {
    private final WalletRepository walletRepository;

    public WalletProvisioner(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    public Wallet createInitialWallet(User user) {
        Wallet wallet = new Wallet(BigDecimal.ZERO);
        user.assignWallet(wallet);
        return walletRepository.save(wallet);
    }
}
