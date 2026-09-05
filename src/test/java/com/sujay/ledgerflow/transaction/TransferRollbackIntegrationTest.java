package com.sujay.ledgerflow.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.sujay.ledgerflow.auth.AuthService;
import com.sujay.ledgerflow.dto.RegisterRequest;
import com.sujay.ledgerflow.repository.TransactionRepository;
import com.sujay.ledgerflow.repository.UserRepository;
import com.sujay.ledgerflow.repository.WalletRepository;
import com.sujay.ledgerflow.user.User;
import com.sujay.ledgerflow.wallet.Wallet;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;

/** Proves that a failed transaction-record write reverses both already-managed wallet mutations. */
@SpringBootTest
@Import(TransferRollbackIntegrationTest.FailingTransactionWriterConfiguration.class)
class TransferRollbackIntegrationTest {
    @Autowired private TransferService transferService;
    @Autowired private AuthService authService;
    @Autowired private UserRepository userRepository;
    @Autowired private WalletRepository walletRepository;

    @Autowired private TransactionRepository transactionRepository;
    @BeforeEach
    void clearDatabase() {
        walletRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void transactionPersistenceFailureRollsBackDebitAndCredit() {
        User sender = register("sender@example.com");
        User recipient = register("recipient@example.com");
        Wallet senderWallet = walletRepository.findByUserId(sender.getId()).orElseThrow();
        senderWallet.credit(new BigDecimal("1000.00"));
        walletRepository.saveAndFlush(senderWallet);
        assertThatThrownBy(() -> transferService.transfer(sender.getId(), new TransferRequest(recipient.getId(), new BigDecimal("500.00"))))
                .isInstanceOf(IllegalStateException.class);

        assertThat(walletRepository.findByUserId(sender.getId()).orElseThrow().getBalance()).isEqualByComparingTo("1000.00");
        assertThat(walletRepository.findByUserId(recipient.getId()).orElseThrow().getBalance()).isEqualByComparingTo("0.00");
        assertThat(transactionRepository.count()).isZero();
    }

    private User register(String email) {
        authService.register(new RegisterRequest("Transfer User", email, "9876543210", "SecurePassword123"));
        return userRepository.findByEmail(email).orElseThrow();
    }

    @TestConfiguration
    static class FailingTransactionWriterConfiguration {
        @Bean
        @Primary
        TransactionWriter failingTransactionWriter() {
            return transaction -> {
                throw new IllegalStateException("transaction storage unavailable");
            };
        }
    }
}
