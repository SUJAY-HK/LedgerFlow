package com.sujay.ledgerflow.wallet;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wallet")
@Tag(name = "Wallet")
@SecurityRequirement(name = "bearerAuth")
public class WalletController {
    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    @Operation(summary = "Get the authenticated user's wallet", responses = {
            @ApiResponse(responseCode = "200", description = "Wallet retrieved"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token"),
            @ApiResponse(responseCode = "404", description = "Wallet not found")})
    public WalletResponse getWallet(@AuthenticationPrincipal Jwt jwt) {
        return walletService.getCurrentUserWallet(UUID.fromString(jwt.getSubject()));
    }

    @GetMapping("/balance")
    @Operation(summary = "Get the authenticated user's current wallet balance", responses = {
            @ApiResponse(responseCode = "200", description = "Balance retrieved"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token"),
            @ApiResponse(responseCode = "404", description = "Wallet not found")})
    public WalletBalanceResponse getBalance(@AuthenticationPrincipal Jwt jwt) {
        return walletService.getCurrentUserBalance(UUID.fromString(jwt.getSubject()));
    }
}
