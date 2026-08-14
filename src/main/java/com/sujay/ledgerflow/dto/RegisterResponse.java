package com.sujay.ledgerflow.dto;

import com.sujay.ledgerflow.user.User;
import com.sujay.ledgerflow.user.UserStatus;
import java.util.UUID;

/** Safe user representation. It contains neither a password nor its hash. */
public record RegisterResponse(UUID id, String name, String email, String phone, UserStatus status) {
    public static RegisterResponse from(User user) {
        return new RegisterResponse(user.getId(), user.getName(), user.getEmail(), user.getPhone(), user.getStatus());
    }
}
