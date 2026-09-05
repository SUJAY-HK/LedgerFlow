package com.sujay.ledgerflow.exception;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException() {
        super("The sender wallet has insufficient balance");
    }
}
