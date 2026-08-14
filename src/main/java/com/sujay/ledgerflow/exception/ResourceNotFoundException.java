package com.sujay.ledgerflow.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource) {
        super(resource + " was not found");
    }
}
