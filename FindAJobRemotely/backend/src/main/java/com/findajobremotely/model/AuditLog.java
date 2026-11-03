package com.findajobremotely.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** Append-only trail of security-relevant and administrative actions. */
@Document("audit_logs")
public class AuditLog {

    @Id
    public String id;

    public String actorId;
    public String actorEmail;
    public String action;
    public String targetType;
    public String targetId;
    public String details;
    public String ip;

    @Indexed
    public Instant createdAt = Instant.now();
}
