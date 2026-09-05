package com.sujay.ledgerflow.repository;

import com.sujay.ledgerflow.wallet.Wallet;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    Optional<Wallet> findByUserId(UUID userId);

    /** Looks up identity without putting a stale Wallet instance in the persistence context. */
    @Query("select w.id from Wallet w where w.user.id = :userId")
    Optional<UUID> findIdByUserId(@Param("userId") UUID userId);

    /** Serializes balance-changing operations on this wallet. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.id = :id")
    Optional<Wallet> findByIdForUpdate(@Param("id") UUID id);
}
