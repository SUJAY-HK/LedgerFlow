package com.sujay.ledgerflow.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.sujay.ledgerflow.auth.AuthService;
import com.sujay.ledgerflow.dto.RegisterRequest;
import com.sujay.ledgerflow.repository.TransactionRepository;
import com.sujay.ledgerflow.repository.UserRepository;
import com.sujay.ledgerflow.repository.WalletRepository;
import com.sujay.ledgerflow.user.User;
import com.sujay.ledgerflow.wallet.Wallet;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TransferConcurrencyIntegrationTest {
    @Autowired private TransferService transferService;
    @Autowired private AuthService authService;
    @Autowired private UserRepository userRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private TransactionRepository transactionRepository;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
        userRepository.deleteAll();
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void shutDown() {
        executor.shutdownNow();
    }

    @Test
    void simultaneousTransfersCannotOverdrawTheSameWallet() throws Exception {
        User sender = register("sender@example.com");
        User recipientOne = register("recipient.one@example.com");
        User recipientTwo = register("recipient.two@example.com");
        Wallet wallet = walletRepository.findByUserId(sender.getId()).orElseThrow();
        wallet.credit(new BigDecimal("1000.00"));
        walletRepository.saveAndFlush(wallet);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<Boolean>> results = List.of(
                executor.submit(() -> attempt(sender, recipientOne, ready, start)),
                executor.submit(() -> attempt(sender, recipientTwo, ready, start)));
        ready.await();
        start.countDown();

        long successes = 0;
        for (Future<Boolean> result : results) {
            if (result.get()) successes++;
        }
        assertThat(successes).isEqualTo(1);
        assertThat(walletRepository.findByUserId(sender.getId()).orElseThrow().getBalance()).isEqualByComparingTo("200.00");
        assertThat(transactionRepository.count()).isEqualTo(1);
    }

    private boolean attempt(User sender, User recipient, CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            transferService.transfer(sender.getId(), new TransferRequest(recipient.getId(), new BigDecimal("800.00")));
            return true;
        } catch (RuntimeException expectedBusinessFailure) {
            return false;
        }
    }

    private User register(String email) {
        authService.register(new RegisterRequest("Transfer User", email, "9876543210", "SecurePassword123"));
        return userRepository.findByEmail(email).orElseThrow();
    }
}
