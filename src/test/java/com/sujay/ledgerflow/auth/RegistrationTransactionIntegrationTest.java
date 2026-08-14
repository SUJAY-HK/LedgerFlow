package com.sujay.ledgerflow.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.sujay.ledgerflow.dto.RegisterRequest;
import com.sujay.ledgerflow.repository.UserRepository;
import com.sujay.ledgerflow.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Proves that a downstream wallet error also rolls back the already-saved user. */
@SpringBootTest
class RegistrationTransactionIntegrationTest {
    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @MockitoBean
    private WalletProvisioner walletProvisioner;

    @BeforeEach
    void clearDatabase() {
        walletRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void walletFailureRollsBackUserCreation() {
        doThrow(new IllegalStateException("wallet storage unavailable"))
                .when(walletProvisioner).createInitialWallet(any());

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Sujay", "rollback@example.com", "9876543210", "SecurePassword123")))
                .isInstanceOf(IllegalStateException.class);

        assertThat(userRepository.findByEmail("rollback@example.com")).isEmpty();
        assertThat(walletRepository.count()).isZero();
    }
}
