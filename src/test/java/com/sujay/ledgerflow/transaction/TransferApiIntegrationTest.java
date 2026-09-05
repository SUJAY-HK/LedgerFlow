package com.sujay.ledgerflow.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sujay.ledgerflow.auth.AuthService;
import com.sujay.ledgerflow.dto.RegisterRequest;
import com.sujay.ledgerflow.repository.TransactionRepository;
import com.sujay.ledgerflow.repository.UserRepository;
import com.sujay.ledgerflow.repository.WalletRepository;
import com.sujay.ledgerflow.security.JwtService;
import com.sujay.ledgerflow.user.User;
import com.sujay.ledgerflow.wallet.Wallet;
import com.sujay.ledgerflow.wallet.WalletStatus;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class TransferApiIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private AuthService authService;
    @Autowired private JwtService jwtService;
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
    void authenticatedUserCanTransferAndRetrieveTheirTransfer() throws Exception {
        User sender = register("sender@example.com");
        User recipient = register("recipient@example.com");
        fund(sender, "1000.00");

        MvcResult created = mockMvc.perform(transfer(sender, recipient.getId(), "500.00"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recipientId").value(recipient.getId().toString()))
                .andExpect(jsonPath("$.amount").value(500.0))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.transactionId").isNotEmpty())
                .andReturn();

        UUID transactionId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(
                created.getResponse().getContentAsString(), "$.transactionId"));
        assertBalance(sender, "500.00");
        assertBalance(recipient, "500.00");
        assertThat(transactionRepository.count()).isEqualTo(1);

        mockMvc.perform(get("/api/v1/transfers/{id}", transactionId).header("Authorization", bearer(sender)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(transactionId.toString()));
    }

    @Test
    void transferEndpointRequiresJwtAndNeverAcceptsSenderIdentityFromRequest() throws Exception {
        User recipient = register("recipient@example.com");
        mockMvc.perform(post("/api/v1/transfers").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipientId\":\"%s\",\"amount\":10.00}".formatted(recipient.getId())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidAmountsAndSelfTransferWithoutChangingBalances() throws Exception {
        User sender = register("sender@example.com");
        User recipient = register("recipient@example.com");
        fund(sender, "100.00");

        mockMvc.perform(transfer(sender, recipient.getId(), "0.00"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
        mockMvc.perform(transfer(sender, recipient.getId(), "-1.00"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
        mockMvc.perform(transfer(sender, recipient.getId(), "1.123"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
        mockMvc.perform(transfer(sender, sender.getId(), "1.00"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("INVALID_TRANSFER"));

        assertBalance(sender, "100.00");
        assertBalance(recipient, "0.00");
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void insufficientBalanceAndAbsentRecipientLeaveNoTransaction() throws Exception {
        User sender = register("sender@example.com");
        fund(sender, "100.00");

        mockMvc.perform(transfer(sender, UUID.randomUUID(), "10.00"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("RECIPIENT_NOT_FOUND"));
        User recipient = register("recipient@example.com");
        mockMvc.perform(transfer(sender, recipient.getId(), "100.01"))
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.error").value("INSUFFICIENT_BALANCE"));

        assertBalance(sender, "100.00");
        assertBalance(recipient, "0.00");
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void restrictedAndClosedWalletsCannotSendOrReceive() throws Exception {
        User sender = register("sender@example.com");
        User recipient = register("recipient@example.com");
        fund(sender, "100.00");

        setStatus(sender, WalletStatus.RESTRICTED);
        mockMvc.perform(transfer(sender, recipient.getId(), "10.00"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("WALLET_TRANSFER_NOT_ALLOWED"));
        setStatus(sender, WalletStatus.ACTIVE);
        setStatus(recipient, WalletStatus.CLOSED);
        mockMvc.perform(transfer(sender, recipient.getId(), "10.00"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("WALLET_TRANSFER_NOT_ALLOWED"));

        assertBalance(sender, "100.00");
        assertBalance(recipient, "0.00");
    }

    @Test
    void usersOutsideTheTransferCannotReadIt() throws Exception {
        User sender = register("sender@example.com");
        User recipient = register("recipient@example.com");
        User outsider = register("outsider@example.com");
        fund(sender, "100.00");
        MvcResult created = mockMvc.perform(transfer(sender, recipient.getId(), "10.00")).andExpect(status().isCreated()).andReturn();
        UUID transactionId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.transactionId"));

        mockMvc.perform(get("/api/v1/transfers/{id}", transactionId).header("Authorization", bearer(outsider)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("TRANSACTION_NOT_FOUND"));
    }

    @Test
    void openApiListsProtectedTransferEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/transfers'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/transfers/{transactionId}'].get").exists());
    }

    private User register(String email) {
        authService.register(new RegisterRequest("Transfer User", email, "9876543210", "SecurePassword123"));
        return userRepository.findByEmail(email).orElseThrow();
    }

    private void fund(User user, String amount) {
        Wallet wallet = walletRepository.findByUserId(user.getId()).orElseThrow();
        wallet.credit(new BigDecimal(amount));
        walletRepository.saveAndFlush(wallet);
    }

    private void setStatus(User user, WalletStatus status) {
        Wallet wallet = walletRepository.findByUserId(user.getId()).orElseThrow();
        wallet.changeStatus(status);
        walletRepository.saveAndFlush(wallet);
    }

    private void assertBalance(User user, String expected) {
        assertThat(walletRepository.findByUserId(user.getId()).orElseThrow().getBalance()).isEqualByComparingTo(expected);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder transfer(User sender, UUID recipientId, String amount) {
        return post("/api/v1/transfers").header("Authorization", bearer(sender)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipientId\":\"%s\",\"amount\":%s}".formatted(recipientId, amount));
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user.getId()).getTokenValue();
    }
}
