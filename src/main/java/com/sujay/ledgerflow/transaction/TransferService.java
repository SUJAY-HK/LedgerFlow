package com.sujay.ledgerflow.transaction;

import com.sujay.ledgerflow.exception.InvalidTransferException;
import com.sujay.ledgerflow.exception.RecipientNotFoundException;
import com.sujay.ledgerflow.exception.TransactionNotFoundException;
import com.sujay.ledgerflow.exception.WalletNotFoundException;
import com.sujay.ledgerflow.exception.WalletTransferNotAllowedException;
import com.sujay.ledgerflow.repository.TransactionRepository;
import com.sujay.ledgerflow.repository.WalletRepository;
import com.sujay.ledgerflow.wallet.Wallet;
import com.sujay.ledgerflow.wallet.WalletStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionWriter transactionWriter;

    public TransferService(
            WalletRepository walletRepository, TransactionRepository transactionRepository, TransactionWriter transactionWriter) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.transactionWriter = transactionWriter;
    }

    /**
     * Locks both wallets in UUID order, mutates their balances, and persists the record in one database transaction.
     * Any runtime exception, including a transaction persistence failure, rolls back every change.
     */
    @Transactional
    public TransferResponse transfer(UUID senderUserId, TransferRequest request) {
        validateRequest(senderUserId, request);

        UUID senderWalletId = walletRepository.findIdByUserId(senderUserId).orElseThrow(WalletNotFoundException::new);
        UUID recipientWalletId = walletRepository.findIdByUserId(request.recipientId()).orElseThrow(RecipientNotFoundException::new);
        WalletPair wallets = lockWallets(senderWalletId, recipientWalletId);

        validateTransferable(wallets.sender());
        validateTransferable(wallets.recipient());
        if (wallets.sender().getCurrency() != wallets.recipient().getCurrency()) {
            throw new InvalidTransferException("Sender and recipient wallets must use the same currency");
        }

        BigDecimal amount = request.amount().setScale(2, RoundingMode.UNNECESSARY);
        wallets.sender().debit(amount);
        wallets.recipient().credit(amount);

        Transaction transaction = transactionWriter.save(
                new Transaction(wallets.sender(), wallets.recipient(), amount, wallets.sender().getCurrency()));
        return response(transaction);
    }

    @Transactional(readOnly = true)
    public TransferResponse getTransfer(UUID transactionId, UUID requestingUserId) {
        Transaction transaction = transactionRepository.findAuthorizedById(transactionId, requestingUserId)
                .orElseThrow(TransactionNotFoundException::new);
        return response(transaction);
    }

    private WalletPair lockWallets(UUID senderWalletId, UUID recipientWalletId) {
        UUID firstId = senderWalletId.compareTo(recipientWalletId) < 0 ? senderWalletId : recipientWalletId;
        UUID secondId = firstId.equals(senderWalletId) ? recipientWalletId : senderWalletId;
        Wallet first = walletRepository.findByIdForUpdate(firstId).orElseThrow(WalletNotFoundException::new);
        Wallet second = walletRepository.findByIdForUpdate(secondId).orElseThrow(RecipientNotFoundException::new);
        return firstId.equals(senderWalletId) ? new WalletPair(first, second) : new WalletPair(second, first);
    }

    private void validateRequest(UUID senderUserId, TransferRequest request) {
        if (request == null || request.recipientId() == null || request.amount() == null) {
            throw new InvalidTransferException("Recipient and amount are required");
        }
        if (senderUserId.equals(request.recipientId())) {
            throw new InvalidTransferException("Sender and recipient must differ");
        }
        if (request.amount().signum() <= 0 || request.amount().scale() > 2 || request.amount().precision() - request.amount().scale() > 17) {
            throw new InvalidTransferException("Amount must be positive with at most 17 whole digits and 2 decimal places");
        }
    }

    private void validateTransferable(Wallet wallet) {
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new WalletTransferNotAllowedException();
        }
    }

    private TransferResponse response(Transaction transaction) {
        return new TransferResponse(
                transaction.getId(), transaction.getRecipientWallet().getUser().getId(), transaction.getAmount(),
                transaction.getCurrency(), transaction.getStatus(), transaction.getCreatedAt());
    }

    private record WalletPair(Wallet sender, Wallet recipient) {}
}
