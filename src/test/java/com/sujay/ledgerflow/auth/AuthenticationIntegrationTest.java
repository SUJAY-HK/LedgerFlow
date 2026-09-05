package com.sujay.ledgerflow.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sujay.ledgerflow.repository.UserRepository;
import com.sujay.ledgerflow.repository.TransactionRepository;
import com.sujay.ledgerflow.repository.WalletRepository;
import com.sujay.ledgerflow.security.JwtService;
import com.sujay.ledgerflow.user.User;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationIntegrationTest {
    private static final String EMAIL = "sujay@example.com";
    private static final String PASSWORD = "SecurePassword123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtEncoder jwtEncoder;

    @BeforeEach
    void clearDatabase() {
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void validRegistrationCreatesUserAndWalletWithoutReturningPasswordData() throws Exception {
        mockMvc.perform(registerRequest(EMAIL, PASSWORD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Sujay"))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        User user = userRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(walletRepository.findAll()).hasSize(1);
        assertThat(walletRepository.findAll().getFirst().getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    void duplicateEmailIsRejectedAsConflict() throws Exception {
        mockMvc.perform(registerRequest(EMAIL, PASSWORD)).andExpect(status().isCreated());

        mockMvc.perform(registerRequest(EMAIL, PASSWORD))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("USER_ALREADY_EXISTS"));
    }

    @Test
    void invalidEmailIsRejected() throws Exception {
        mockMvc.perform(registerRequest("not-an-email", PASSWORD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void blankPasswordIsRejected() throws Exception {
        mockMvc.perform(registerRequest(EMAIL, ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void passwordIsStoredAsBcryptHashNotPlaintext() throws Exception {
        mockMvc.perform(registerRequest(EMAIL, PASSWORD)).andExpect(status().isCreated());

        String storedHash = userRepository.findByEmail(EMAIL).orElseThrow().getPasswordHash();
        assertThat(storedHash).startsWith("$2").isNotEqualTo(PASSWORD);
    }

    @Test
    void validCredentialsReturnJwtWithExpectedIdentityClaims() throws Exception {
        mockMvc.perform(registerRequest(EMAIL, PASSWORD)).andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(loginRequest(EMAIL, PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        Jwt jwt = jwtService.generateToken(userRepository.findByEmail(EMAIL).orElseThrow().getId());
        String returnedToken = JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
        Jwt decoded = org.springframework.security.oauth2.jwt.NimbusJwtDecoder
                .withSecretKey(new javax.crypto.spec.SecretKeySpec(
                        "test-only-secret-that-is-at-least-thirty-two-bytes-long".getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"))
                .macAlgorithm(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build().decode(returnedToken);
        assertThat(decoded.getSubject()).isEqualTo(userRepository.findByEmail(EMAIL).orElseThrow().getId().toString());
        assertThat(decoded.getIssuedAt()).isNotNull();
        assertThat(decoded.getExpiresAt()).isAfter(decoded.getIssuedAt());
        assertThat(jwt.getSubject()).isEqualTo(decoded.getSubject());
    }

    @Test
    void wrongPasswordAndUnknownEmailAreIndistinguishableAuthenticationFailures() throws Exception {
        mockMvc.perform(registerRequest(EMAIL, PASSWORD)).andExpect(status().isCreated());

        mockMvc.perform(loginRequest(EMAIL, "WrongPassword123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
        mockMvc.perform(loginRequest("unknown@example.com", PASSWORD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void protectedEndpointRejectsMissingAndInvalidTokens() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer not-a-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointRejectsExpiredToken() throws Exception {
        mockMvc.perform(registerRequest(EMAIL, PASSWORD)).andExpect(status().isCreated());
        String subject = userRepository.findByEmail(EMAIL).orElseThrow().getId().toString();
        Instant issuedAt = Instant.now().minusSeconds(120);
        String expiredToken = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder().issuer("ledgerflow-test").subject(subject).issuedAt(issuedAt)
                        .expiresAt(issuedAt.plusSeconds(30)).build())).getTokenValue();

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointAllowsValidJwt() throws Exception {
        mockMvc.perform(registerRequest(EMAIL, PASSWORD)).andExpect(status().isCreated());
        String token = tokenFor(EMAIL, PASSWORD);

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    void openApiPublishesBearerAuthenticationScheme() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
    }

    private String tokenFor(String email, String password) throws Exception {
        MvcResult login = mockMvc.perform(loginRequest(email, password)).andExpect(status().isOk()).andReturn();
        return JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder registerRequest(String email, String password) {
        return post("/api/v1/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Sujay","email":"%s","phone":"9876543210","password":"%s"}
                        """.formatted(email, password));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder loginRequest(String email, String password) {
        return post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(email, password));
    }
}
