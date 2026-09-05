package com.sujay.ledgerflow.repository;

import com.sujay.ledgerflow.transaction.Transaction;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /** Finds a transaction only when the requesting user was either party to it. */
    @EntityGraph(attributePaths = {"senderWallet", "recipientWallet"})
    @Query("""
            select t from Transaction t
            join t.senderWallet sender
            join t.recipientWallet recipient
            where t.id = :transactionId
              and (sender.user.id = :userId or recipient.user.id = :userId)
            """)
    Optional<Transaction> findAuthorizedById(@Param("transactionId") UUID transactionId, @Param("userId") UUID userId);
}
