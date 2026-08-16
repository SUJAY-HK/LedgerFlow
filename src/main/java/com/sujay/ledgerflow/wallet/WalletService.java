package com.sujay.ledgerflow.wallet;

import com.sujay.ledgerflow.exception.WalletNotFoundException;
import com.sujay.ledgerflow.mapper.WalletMapper;
import com.sujay.ledgerflow.repository.WalletRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {
    private final WalletRepository walletRepository;
    private final WalletMapper walletMapper;

    public WalletService(WalletRepository walletRepository, WalletMapper walletMapper) {
        this.walletRepository = walletRepository;
        this.walletMapper = walletMapper;
    }

    @Transactional(readOnly = true)
    public WalletResponse getCurrentUserWallet(UUID userId) {
        Wallet wallet = findOwnedWallet(userId);
        verifyReadable(wallet);
        return walletMapper.toResponse(wallet);
    }

    @Transactional(readOnly = true)
    public WalletBalanceResponse getCurrentUserBalance(UUID userId) {
        Wallet wallet = findOwnedWallet(userId);
        verifyReadable(wallet);
        return walletMapper.toBalanceResponse(wallet);
    }

    private Wallet findOwnedWallet(UUID userId) {
        Wallet wallet = walletRepository.findByUserId(userId).orElseThrow(WalletNotFoundException::new);
        // Derived lookup provides the primary guarantee. Retain this check as defense in depth for repository changes.
        if (wallet.getUser() == null || !userId.equals(wallet.getUser().getId())) {
            throw new WalletNotFoundException();
        }
        return wallet;
    }

    private void verifyReadable(Wallet wallet) {
        switch (wallet.getStatus()) {
            case ACTIVE, RESTRICTED, CLOSED -> {
                // All lifecycle states are readable in Sprint 4; only future money operations will differ.
            }
        }
    }
}
