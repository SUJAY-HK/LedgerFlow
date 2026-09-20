package com.sujay.ledgerflow.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sujay.ledgerflow.auth.AuthService;
import com.sujay.ledgerflow.dto.RegisterRequest;
import com.sujay.ledgerflow.repository.TransactionRepository;
import com.sujay.ledgerflow.repository.UserRepository;
import com.sujay.ledgerflow.repository.WalletRepository;
import com.sujay.ledgerflow.transaction.TransactionWriter;
import com.sujay.ledgerflow.user.User;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

/** Proves a failed deposit-record write also reverses the wallet credit. */
@SpringBootTest
@Import(DepositRollbackIntegrationTest.FailingTransactionWriterConfiguration.class)
class DepositRollbackIntegrationTest {
    @Autowired private WalletService walletService;
    @Autowired private AuthService authService;
    @Autowired private UserRepository userRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private TransactionRepository transactionRepository;

    @BeforeEach
    void clearDatabase() {
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void transactionPersistenceFailureRollsBackWalletCredit() {
        User user = register("deposit.rollback@example.com");

        assertThatThrownBy(() -> walletService.deposit(user.getId(), new DepositRequest(new BigDecimal("1000.00"))))
                .isInstanceOf(IllegalStateException.class);

        assertThat(walletRepository.findByUserId(user.getId()).orElseThrow().getBalance()).isEqualByComparingTo("0.00");
        assertThat(transactionRepository.count()).isZero();
    }

    private User register(String email) {
        authService.register(new RegisterRequest("Deposit User", email, "9876543210", "SecurePassword123"));
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
