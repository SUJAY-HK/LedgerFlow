package com.sujay.ledgerflow.transaction;

/** Writes completed transfer records as part of the caller's database transaction. */
public interface TransactionWriter {
    Transaction save(Transaction transaction);
}
