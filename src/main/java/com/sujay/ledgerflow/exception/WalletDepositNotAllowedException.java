package com.sujay.ledgerflow.exception;

public class WalletDepositNotAllowedException extends RuntimeException {
    public WalletDepositNotAllowedException() {
        super("Only ACTIVE wallets may receive deposits");
    }
}
