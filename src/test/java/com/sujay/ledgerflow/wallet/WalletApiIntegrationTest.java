package com.sujay.ledgerflow.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sujay.ledgerflow.auth.AuthService;
import com.sujay.ledgerflow.dto.RegisterRequest;
import com.sujay.ledgerflow.repository.UserRepository;
import com.sujay.ledgerflow.repository.TransactionRepository;
import com.sujay.ledgerflow.repository.WalletRepository;
import com.sujay.ledgerflow.security.JwtService;
import com.sujay.ledgerflow.transaction.TransactionType;
import com.sujay.ledgerflow.user.User;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class WalletApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void clearDatabase() {
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void authenticatedUserCanGetWalletAndBalance() throws Exception {
        User user = register("wallet.owner@example.com");
        String token = tokenFor(user);
        Wallet wallet = walletRepository.findByUserId(user.getId()).orElseThrow();

        mockMvc.perform(get("/api/v1/wallet").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value(wallet.getId().toString()))
                .andExpect(jsonPath("$.balance").value(0.0))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/api/v1/wallet/balance").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(0.0))
                .andExpect(jsonPath("$.currency").value("INR"));
    }

    @Test
    void walletEndpointsRequireJwt() throws Exception {
        mockMvc.perform(get("/api/v1/wallet")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/wallet/balance")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/wallet/deposits").contentType(MediaType.APPLICATION_JSON).content("{\"amount\":1000.00}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanDepositAndReceiveAnAuditableRecord() throws Exception {
        User user = register("depositor@example.com");

        mockMvc.perform(deposit(user, "1000.00"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").isNotEmpty())
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount").value(1000.0))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.balance").value(1000.0));

        Wallet wallet = walletRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(wallet.getBalance()).isEqualByComparingTo("1000.00");
        assertThat(transactionRepository.count()).isEqualTo(1);
        assertThat(transactionRepository.findAll().getFirst().getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(transactionRepository.findAll().getFirst().getSenderWallet()).isNull();
        assertThat(transactionRepository.findAll().getFirst().getRecipientWallet().getId()).isEqualTo(wallet.getId());
    }

    @Test
    void depositRejectsInvalidAmountsWithoutChangingBalanceOrCreatingARecord() throws Exception {
        User user = register("invalid.deposit@example.com");

        mockMvc.perform(deposit(user, "0.00"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
        mockMvc.perform(deposit(user, "-1.00"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
        mockMvc.perform(deposit(user, "100.999"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

        assertThat(walletRepository.findByUserId(user.getId()).orElseThrow().getBalance()).isEqualByComparingTo("0.00");
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void depositUsesOnlyTheWalletIdentifiedByTheJwt() throws Exception {
        User userA = register("deposit.a@example.com");
        User userB = register("deposit.b@example.com");

        mockMvc.perform(deposit(userA, "250.00")).andExpect(status().isCreated());

        assertThat(walletRepository.findByUserId(userA.getId()).orElseThrow().getBalance()).isEqualByComparingTo("250.00");
        assertThat(walletRepository.findByUserId(userB.getId()).orElseThrow().getBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void restrictedAndClosedWalletsCannotReceiveDeposits() throws Exception {
        User user = register("deposit.status@example.com");
        Wallet wallet = walletRepository.findByUserId(user.getId()).orElseThrow();

        wallet.changeStatus(WalletStatus.RESTRICTED);
        walletRepository.saveAndFlush(wallet);
        mockMvc.perform(deposit(user, "100.00"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("WALLET_DEPOSIT_NOT_ALLOWED"));

        wallet.changeStatus(WalletStatus.CLOSED);
        walletRepository.saveAndFlush(wallet);
        mockMvc.perform(deposit(user, "100.00"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("WALLET_DEPOSIT_NOT_ALLOWED"));

        assertThat(walletRepository.findByUserId(user.getId()).orElseThrow().getBalance()).isEqualByComparingTo("0.00");
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void userAJwtAlwaysReturnsWalletAAndNeverWalletB() throws Exception {
        User userA = register("user.a@example.com");
        User userB = register("user.b@example.com");
        Wallet walletA = walletRepository.findByUserId(userA.getId()).orElseThrow();
        Wallet walletB = walletRepository.findByUserId(userB.getId()).orElseThrow();

        mockMvc.perform(get("/api/v1/wallet").header("Authorization", "Bearer " + tokenFor(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value(walletA.getId().toString()))
                .andExpect(jsonPath("$.walletId").value(org.hamcrest.Matchers.not(walletB.getId().toString())));
    }

    @Test
    void restrictedAndClosedWalletsAreReadable() throws Exception {
        User user = register("status@example.com");
        Wallet wallet = walletRepository.findByUserId(user.getId()).orElseThrow();
        wallet.changeStatus(WalletStatus.RESTRICTED);
        walletRepository.saveAndFlush(wallet);

        mockMvc.perform(get("/api/v1/wallet").header("Authorization", "Bearer " + tokenFor(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESTRICTED"));

        wallet.changeStatus(WalletStatus.CLOSED);
        walletRepository.saveAndFlush(wallet);
        mockMvc.perform(get("/api/v1/wallet/balance").header("Authorization", "Bearer " + tokenFor(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(0.0));
    }

    @Test
    void reportsNotFoundWhenAuthenticatedUserUnexpectedlyHasNoWallet() throws Exception {
        User user = new User("No Wallet", "no.wallet@example.com", "9876543210", "$2a$10$hash");
        user = userRepository.saveAndFlush(user);

        mockMvc.perform(get("/api/v1/wallet").header("Authorization", "Bearer " + tokenFor(user)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("WALLET_NOT_FOUND"));
    }

    @Test
    void openApiListsProtectedWalletEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/wallet'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/wallet/balance'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/wallet/deposits'].post").exists());
    }

    private User register(String email) {
        authService.register(new RegisterRequest("Wallet User", email, "9876543210", "SecurePassword123"));
        return userRepository.findByEmail(email).orElseThrow();
    }

    private String tokenFor(User user) {
        return jwtService.generateToken(user.getId()).getTokenValue();
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder deposit(User user, String amount) {
        return post("/api/v1/wallet/deposits")
                .header("Authorization", "Bearer " + tokenFor(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":%s}".formatted(amount));
    }
}
