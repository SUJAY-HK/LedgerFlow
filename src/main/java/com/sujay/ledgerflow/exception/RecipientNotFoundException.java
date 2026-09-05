package com.sujay.ledgerflow.exception;

public class RecipientNotFoundException extends RuntimeException {
    public RecipientNotFoundException() {
        super("The recipient does not have a wallet");
    }
}
