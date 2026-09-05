package com.sujay.ledgerflow.transaction;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transfers")
@Tag(name = "Transfers")
@SecurityRequirement(name = "bearerAuth")
public class TransferController {
    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Transfer funds from the authenticated user's wallet", responses = {
            @ApiResponse(responseCode = "201", description = "Transfer completed"),
            @ApiResponse(responseCode = "400", description = "Invalid amount or self transfer"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token"),
            @ApiResponse(responseCode = "404", description = "Sender or recipient wallet not found"),
            @ApiResponse(responseCode = "409", description = "A wallet cannot transfer in its current status"),
            @ApiResponse(responseCode = "422", description = "Insufficient sender balance")})
    public TransferResponse createTransfer(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TransferRequest request) {
        return transferService.transfer(UUID.fromString(jwt.getSubject()), request);
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Get a transfer involving the authenticated user", responses = {
            @ApiResponse(responseCode = "200", description = "Transfer retrieved"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token"),
            @ApiResponse(responseCode = "404", description = "Transfer not found or not accessible")})
    public TransferResponse getTransfer(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID transactionId) {
        return transferService.getTransfer(transactionId, UUID.fromString(jwt.getSubject()));
    }
}
