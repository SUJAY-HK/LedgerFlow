package com.sujay.ledgerflow.exception;

import com.sujay.ledgerflow.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException exception, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "USER_ALREADY_EXISTS", exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException exception, HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", exception.getMessage(), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException exception, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler(WalletNotFoundException.class)
    ResponseEntity<ErrorResponse> handleWalletNotFound(WalletNotFoundException exception, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "WALLET_NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler(RecipientNotFoundException.class)
    ResponseEntity<ErrorResponse> handleRecipientNotFound(RecipientNotFoundException exception, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "RECIPIENT_NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    ResponseEntity<ErrorResponse> handleTransactionNotFound(TransactionNotFoundException exception, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "TRANSACTION_NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    ResponseEntity<ErrorResponse> handleInsufficientBalance(InsufficientBalanceException exception, HttpServletRequest request) {
        return response(HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_BALANCE", exception.getMessage(), request);
    }

    @ExceptionHandler({InvalidTransferException.class, IllegalArgumentException.class})
    ResponseEntity<ErrorResponse> handleInvalidTransfer(RuntimeException exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_TRANSFER", exception.getMessage(), request);
    }

    @ExceptionHandler(WalletTransferNotAllowedException.class)
    ResponseEntity<ErrorResponse> handleWalletTransferNotAllowed(WalletTransferNotAllowedException exception, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "WALLET_TRANSFER_NOT_ALLOWED", exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        ErrorResponse body = new ErrorResponse(java.time.Instant.now(), HttpStatus.BAD_REQUEST.value(), "VALIDATION_FAILED",
                "Request validation failed", request.getRequestURI(), fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    /** Covers a unique-index race that occurs after the service's existence check. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> handleDataIntegrityViolation(HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "CONFLICT", "The request conflicts with existing data", request);
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, String error, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status.value(), error, message, request.getRequestURI()));
    }
}
