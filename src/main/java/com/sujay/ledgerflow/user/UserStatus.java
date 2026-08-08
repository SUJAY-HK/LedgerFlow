package com.sujay.ledgerflow.user;

/** Lifecycle states that determine whether a user may use LedgerFlow. */
public enum UserStatus {
    ACTIVE,
    SUSPENDED,
    BLOCKED,
    DEACTIVATED
}
