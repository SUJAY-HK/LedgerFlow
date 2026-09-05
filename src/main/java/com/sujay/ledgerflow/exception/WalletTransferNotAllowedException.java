package com.sujay.ledgerflow.exception;

public class WalletTransferNotAllowedException extends RuntimeException {
    public WalletTransferNotAllowedException() {
        super("Only ACTIVE wallets may send or receive transfers");
    }
}
