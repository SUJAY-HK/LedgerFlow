package com.sujay.ledgerflow.user;

import com.sujay.ledgerflow.dto.RegisterResponse;
import com.sujay.ledgerflow.exception.ResourceNotFoundException;
import com.sujay.ledgerflow.repository.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class UserProfileService {
    private final UserRepository userRepository;

    public UserProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public RegisterResponse currentUser(String subject) {
        UUID userId;
        try {
            userId = UUID.fromString(subject);
        } catch (IllegalArgumentException exception) {
            throw new ResourceNotFoundException("User");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User"));
        return RegisterResponse.from(user);
    }
}
