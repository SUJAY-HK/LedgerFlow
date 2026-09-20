package com.sujay.ledgerflow.wallet;

import com.sujay.ledgerflow.exception.InvalidDepositException;
import com.sujay.ledgerflow.exception.WalletDepositNotAllowedException;
import com.sujay.ledgerflow.exception.WalletNotFoundException;
import com.sujay.ledgerflow.mapper.WalletMapper;
import com.sujay.ledgerflow.repository.WalletRepository;
import com.sujay.ledgerflow.transaction.Transaction;
import com.sujay.ledgerflow.transaction.TransactionWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {
    private final WalletRepository walletRepository;
    private final WalletMapper walletMapper;
    private final TransactionWriter transactionWriter;

    public WalletService(WalletRepository walletRepository, WalletMapper walletMapper, TransactionWriter transactionWriter) {
        this.walletRepository = walletRepository;
        this.walletMapper = walletMapper;
        this.transactionWriter = transactionWriter;
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

    /**
     * Records simulated external funding and its balance increase as one database transaction.
     * A failure while saving the record rolls back the managed wallet mutation.
     */
    @Transactional
    public DepositResponse deposit(UUID userId, DepositRequest request) {
        validateDeposit(request);
        UUID walletId = walletRepository.findIdByUserId(userId).orElseThrow(WalletNotFoundException::new);
        Wallet wallet = walletRepository.findByIdForUpdate(walletId).orElseThrow(WalletNotFoundException::new);
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new WalletDepositNotAllowedException();
        }

        BigDecimal amount = request.amount().setScale(2, RoundingMode.UNNECESSARY);
        wallet.credit(amount);
        Transaction transaction = transactionWriter.save(Transaction.deposit(wallet, amount, wallet.getCurrency()));
        return new DepositResponse(
                transaction.getId(), transaction.getType(), transaction.getAmount(), transaction.getCurrency(), transaction.getStatus(),
                transaction.getCreatedAt(), walletMapper.toBalanceResponse(wallet).balance());
    }

    private void validateDeposit(DepositRequest request) {
        if (request == null || request.amount() == null
                || request.amount().signum() <= 0
                || request.amount().scale() > 2
                || request.amount().precision() - request.amount().scale() > 17) {
            throw new InvalidDepositException("Amount must be positive with at most 17 whole digits and 2 decimal places");
        }
    }

    private Wallet findWalletForUser(UUID userId) {
        return walletRepository.findByUserId(userId).orElseThrow(WalletNotFoundException::new);
    }
}
