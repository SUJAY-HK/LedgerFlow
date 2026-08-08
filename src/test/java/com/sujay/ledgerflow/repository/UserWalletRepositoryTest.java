package com.sujay.ledgerflow.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sujay.ledgerflow.user.User;
import com.sujay.ledgerflow.wallet.Wallet;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class UserWalletRepositoryTest {

    private static final String PASSWORD_HASH = "$2a$10$ledgerflow.test.password.hash.value.is.not.plaintext";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Test
    void persistsUserAndAutomaticallyAuditsTimestamps() {
        User user = newUser("ada@example.com");
        user.assignWallet(new Wallet(new BigDecimal("0.00")));

        User savedUser = userRepository.saveAndFlush(user);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();

        savedUser.changeName("Ada King");
        User updatedUser = userRepository.saveAndFlush(savedUser);

        assertThat(updatedUser.getUpdatedAt()).isAfterOrEqualTo(updatedUser.getCreatedAt());
    }

    @Test
    void persistsWalletWithItsUserRelationship() {
        User user = newUser("wallet.owner@example.com");
        Wallet wallet = new Wallet(new BigDecimal("125.50"));
        user.assignWallet(wallet);

        User savedUser = userRepository.saveAndFlush(user);
        Wallet savedWallet = walletRepository.findById(wallet.getId()).orElseThrow();

        assertThat(savedWallet.getUser().getId()).isEqualTo(savedUser.getId());
        assertThat(savedUser.getWallet().getId()).isEqualTo(savedWallet.getId());
        assertThat(savedWallet.getBalance()).isEqualByComparingTo("125.50");
        assertThat(savedWallet.getCreatedAt()).isNotNull();
        assertThat(savedWallet.getUpdatedAt()).isNotNull();
    }

    @Test
    void rejectsDuplicateEmailAddresses() {
        User firstUser = newUser("duplicate@example.com");
        firstUser.assignWallet(new Wallet(BigDecimal.ZERO));
        userRepository.saveAndFlush(firstUser);

        User duplicateUser = newUser("duplicate@example.com");
        duplicateUser.assignWallet(new Wallet(BigDecimal.ZERO));

        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicateUser))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsMoreThanOneWalletForTheSameUser() {
        User user = newUser("one.wallet@example.com");
        user.assignWallet(new Wallet(BigDecimal.ZERO));
        userRepository.saveAndFlush(user);

        Wallet secondWallet = new Wallet(BigDecimal.ZERO);
        secondWallet.assignTo(user);

        assertThatThrownBy(() -> walletRepository.saveAndFlush(secondWallet))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private User newUser(String email) {
        return new User("Ada Lovelace", email, "+919876543210", PASSWORD_HASH);
    }
}
