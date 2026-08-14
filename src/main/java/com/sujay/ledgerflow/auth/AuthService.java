package com.sujay.ledgerflow.auth;

import com.sujay.ledgerflow.dto.LoginRequest;
import com.sujay.ledgerflow.dto.LoginResponse;
import com.sujay.ledgerflow.dto.RegisterRequest;
import com.sujay.ledgerflow.dto.RegisterResponse;
import com.sujay.ledgerflow.exception.InvalidCredentialsException;
import com.sujay.ledgerflow.exception.UserAlreadyExistsException;
import com.sujay.ledgerflow.repository.UserRepository;
import com.sujay.ledgerflow.security.JwtService;
import com.sujay.ledgerflow.user.User;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final WalletProvisioner walletProvisioner;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, WalletProvisioner walletProvisioner,
            PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.walletProvisioner = walletProvisioner;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** User and wallet creation must commit or roll back together. */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = canonicalEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException();
        }

        User user = userRepository.save(new User(request.name().trim(), email, request.phone(), passwordEncoder.encode(request.password())));
        walletProvisioner.createInitialWallet(user);
        return RegisterResponse.from(user);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(canonicalEmail(request.email()))
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        Jwt jwt = jwtService.generateToken(user.getId());
        return new LoginResponse(jwt.getTokenValue(), "Bearer", jwt.getExpiresAt());
    }

    private String canonicalEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
