package com.sujay.ledgerflow.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.sujay.ledgerflow.exception.WalletNotFoundException;
import com.sujay.ledgerflow.mapper.WalletMapper;
import com.sujay.ledgerflow.repository.WalletRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {
    private static final UUID USER_ID = UUID.fromString("d2719bf9-1df3-4a0f-9fe2-d1dc3a8e4bd6");

    @Mock
    private WalletRepository walletRepository;

    private WalletService walletService;

    @BeforeEach
    void setUp() {
        walletService = new WalletService(walletRepository, new WalletMapper());
    }

    @Test
    void returnsActiveWalletForAuthenticatedUser() {
        Wallet wallet = walletWith(new BigDecimal("1000.00"), WalletStatus.ACTIVE);
        when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

        WalletResponse response = walletService.getCurrentUserWallet(USER_ID);

        assertThat(response.balance()).isEqualByComparingTo("1000.00");
        assertThat(response.currency()).isEqualTo(WalletCurrency.INR);
        assertThat(response.status()).isEqualTo(WalletStatus.ACTIVE);
    }

    @Test
    void throwsWhenAuthenticatedUserHasNoWallet() {
        when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.getCurrentUserWallet(USER_ID))
                .isInstanceOf(WalletNotFoundException.class);
    }

    @ParameterizedTest
    @EnumSource(value = WalletStatus.class, names = {"RESTRICTED", "CLOSED"})
    void restrictedAndClosedWalletsRemainReadable(WalletStatus status) {
        Wallet wallet = walletWith(BigDecimal.ZERO, status);
        when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

        WalletResponse response = walletService.getCurrentUserWallet(USER_ID);

        assertThat(response.status()).isEqualTo(status);
    }

    @Test
    void mapsBalanceWithExactTwoDecimalPlaces() {
        Wallet wallet = walletWith(new BigDecimal("125.50"), WalletStatus.ACTIVE);
        when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

        WalletBalanceResponse response = walletService.getCurrentUserBalance(USER_ID);

        assertThat(response.balance()).isEqualByComparingTo("125.50");
        assertThat(response.balance().scale()).isEqualTo(2);
        assertThat(response.currency()).isEqualTo(WalletCurrency.INR);
    }

    private Wallet walletWith(BigDecimal balance, WalletStatus status) {
        Wallet wallet = new Wallet(balance);
        wallet.changeStatus(status);
        return wallet;
    }
}
