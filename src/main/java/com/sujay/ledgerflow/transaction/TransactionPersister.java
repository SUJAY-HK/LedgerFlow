package com.sujay.ledgerflow.transaction;

import com.sujay.ledgerflow.repository.TransactionRepository;
import org.springframework.stereotype.Service;

/** Narrow persistence seam that keeps the transfer operation testable without weakening its transaction boundary. */
@Service
public class TransactionPersister implements TransactionWriter {
    private final TransactionRepository transactionRepository;

    public TransactionPersister(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public Transaction save(Transaction transaction) {
        return transactionRepository.save(transaction);
    }
}
