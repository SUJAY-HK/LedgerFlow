package com.sujay.ledgerflow.repository;

import com.sujay.ledgerflow.user.User;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
}
