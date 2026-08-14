package com.sujay.ledgerflow.security;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Service;

/** Issues application JWTs and provides focused token inspection operations. */
@Service
public class JwtService {
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final String issuer;
    private final Duration expiration;

    public JwtService(JwtEncoder jwtEncoder, JwtDecoder jwtDecoder,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.expiration}") Duration expiration) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.issuer = issuer;
        this.expiration = expiration;
    }

    public Jwt generateToken(UUID userId) {
        Instant issuedAt = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(userId.toString())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(expiration))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims));
    }

    /** Validates the signature, issuer, and timestamps through the configured decoder. */
    public boolean isValid(String token) {
        try {
            jwtDecoder.decode(token);
            return true;
        } catch (org.springframework.security.oauth2.jwt.JwtException exception) {
            return false;
        }
    }

    public String extractSubject(String token) {
        return jwtDecoder.decode(token).getSubject();
    }
}
