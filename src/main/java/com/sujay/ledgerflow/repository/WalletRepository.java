package com.sujay.ledgerflow.repository;

import com.sujay.ledgerflow.wallet.Wallet;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {
}
