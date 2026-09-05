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
        Wallet wallet = findWalletForUser(userId);
        return walletMapper.toResponse(wallet);
    }

    @Transactional(readOnly = true)
    public WalletBalanceResponse getCurrentUserBalance(UUID userId) {
        Wallet wallet = findWalletForUser(userId);
        return walletMapper.toBalanceResponse(wallet);
    }

    private Wallet findWalletForUser(UUID userId) {
        return walletRepository.findByUserId(userId).orElseThrow(WalletNotFoundException::new);
    }
}
