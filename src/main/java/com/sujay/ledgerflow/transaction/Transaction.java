package com.sujay.ledgerflow.transaction;

import com.sujay.ledgerflow.wallet.Wallet;
import com.sujay.ledgerflow.wallet.WalletCurrency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** Immutable business record created only after both wallet mutations are valid. */
@Entity
@Table(name = "wallet_transactions")
@EntityListeners(AuditingEntityListener.class)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_wallet_id", nullable = false, updatable = false)
    private Wallet senderWallet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_wallet_id", nullable = false, updatable = false)
    private Wallet recipientWallet;

    @Column(nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal amount;

    /** Captures the currency of the amount, so the record stays intelligible if multi-currency is added. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3, updatable = false)
    private WalletCurrency currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private TransactionStatus status = TransactionStatus.COMPLETED;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Transaction() {
        // Required by JPA.
    }

    public Transaction(Wallet senderWallet, Wallet recipientWallet, BigDecimal amount, WalletCurrency currency) {
        this.senderWallet = senderWallet;
        this.recipientWallet = recipientWallet;
        this.amount = amount;
        this.currency = currency;
    }

    public UUID getId() { return id; }
    public Wallet getSenderWallet() { return senderWallet; }
    public Wallet getRecipientWallet() { return recipientWallet; }
    public BigDecimal getAmount() { return amount; }
    public WalletCurrency getCurrency() { return currency; }
    public TransactionStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
