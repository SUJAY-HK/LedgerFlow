package com.sujay.ledgerflow.dto;

import java.time.Instant;

public record LoginResponse(String accessToken, String tokenType, Instant expiresAt) {
}
