package com.findajobremotely.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** Server-side record of an issued refresh token. Only the SHA-256 hash of the token is stored. */
@Document("refresh_tokens")
public class RefreshToken {

    @Id
    public String id;

    @Indexed(unique = true)
    public String tokenHash;

    @Indexed
    public String userId;

    /** All tokens descended from one login share a family so a stolen token can kill the whole chain. */
    @Indexed
    public String familyId;

    public boolean used;
    public Instant usedAt;

    /** MongoDB removes the document automatically once this moment has passed. */
    @Indexed(expireAfter = "0s")
    public Instant expiresAt;

    public Instant createdAt = Instant.now();
}
