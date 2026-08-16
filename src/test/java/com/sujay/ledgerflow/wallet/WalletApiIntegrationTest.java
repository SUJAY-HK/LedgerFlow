package com.sujay.ledgerflow.wallet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sujay.ledgerflow.auth.AuthService;
import com.sujay.ledgerflow.dto.RegisterRequest;
import com.sujay.ledgerflow.repository.UserRepository;
import com.sujay.ledgerflow.repository.WalletRepository;
import com.sujay.ledgerflow.security.JwtService;
import com.sujay.ledgerflow.user.User;
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

    @BeforeEach
    void clearDatabase() {
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
                .andExpect(jsonPath("$.paths['/api/v1/wallet/balance'].get").exists());
    }

    private User register(String email) {
        authService.register(new RegisterRequest("Wallet User", email, "9876543210", "SecurePassword123"));
        return userRepository.findByEmail(email).orElseThrow();
    }

    private String tokenFor(User user) {
        return jwtService.generateToken(user.getId()).getTokenValue();
    }
}
