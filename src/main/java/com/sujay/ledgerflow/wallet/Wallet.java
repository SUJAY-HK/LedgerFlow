package com.sujay.ledgerflow.wallet;

import com.sujay.ledgerflow.user.User;
import com.sujay.ledgerflow.exception.InsufficientBalanceException;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "wallets")
@EntityListeners(AuditingEntityListener.class)
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @DecimalMin(value = "0.00")
    @Digits(integer = 17, fraction = 2)
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3, updatable = false)
    private WalletCurrency currency = WalletCurrency.INR;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalletStatus status = WalletStatus.ACTIVE;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    protected Wallet() {
        // Required by JPA.
    }

    public Wallet(BigDecimal openingBalance) {
        if (openingBalance == null || openingBalance.signum() < 0 || openingBalance.scale() > 2) {
            throw new IllegalArgumentException("Opening balance must be non-negative with at most two decimal places");
        }
        this.balance = openingBalance.setScale(2, RoundingMode.UNNECESSARY);
    }

    /** Sets the owning side of the relationship. Called by {@link User#assignWallet(Wallet)}. */
    public void assignTo(User user) {
        if (this.user != null && this.user != user) {
            throw new IllegalStateException("A wallet cannot be reassigned to another user");
        }
        this.user = user;
    }

    /** Changes lifecycle state. Read access remains permitted for every status. */
    public void changeStatus(WalletStatus status) {
        this.status = status;
    }

    /** Debits a validated amount while preserving the non-negative balance invariant. */
    public void debit(BigDecimal amount) {
        validateTransferAmount(amount);
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException();
        }
        balance = balance.subtract(amount).setScale(2, RoundingMode.UNNECESSARY);
    }

    /** Credits a validated amount. Callers must have checked the wallet is transferable. */
    public void credit(BigDecimal amount) {
        validateTransferAmount(amount);
        balance = balance.add(amount).setScale(2, RoundingMode.UNNECESSARY);
    }

    private void validateTransferAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2) {
            throw new IllegalArgumentException("Transfer amount must be positive with at most two decimal places");
        }
    }

    public UUID getId() {
        return id;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public WalletCurrency getCurrency() {
        return currency;
    }

    public WalletStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public User getUser() {
        return user;
    }
}
