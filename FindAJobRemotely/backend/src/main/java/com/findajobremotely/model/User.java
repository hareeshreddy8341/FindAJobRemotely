package com.findajobremotely.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** Login identity. Never serialised to clients directly - always mapped to a view DTO. */
@Document("users")
public class User {

    @Id
    public String id;

    @Indexed(unique = true)
    public String email;

    public String passwordHash;
    public String fullName;
    public Role role;
    public AccountStatus status = AccountStatus.ACTIVE;

    public int failedLoginAttempts;
    public Instant lockedUntil;
    public Instant lastLoginAt;

    public Instant createdAt = Instant.now();
    public Instant updatedAt = Instant.now();
}
