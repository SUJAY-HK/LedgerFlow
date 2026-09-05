package com.sujay.ledgerflow.exception;

public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException() {
        super("Transfer not found");
    }
}
