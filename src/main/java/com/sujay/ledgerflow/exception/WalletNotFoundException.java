package com.sujay.ledgerflow.exception;

public class WalletNotFoundException extends RuntimeException {
    public WalletNotFoundException() {
        super("Wallet was not found for the authenticated user");
    }
}
