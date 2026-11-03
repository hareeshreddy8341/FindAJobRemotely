package com.findajobremotely.repository;

import com.findajobremotely.model.AccountStatus;
import com.findajobremotely.model.Role;
import com.findajobremotely.model.User;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);

    long countByRole(Role role);

    long countByStatus(AccountStatus status);
}
