package com.findajobremotely.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Encrypted PDF resume, one per candidate (id = the candidate's user id). */
@Document("resume_files")
public class ResumeFile {

    @Id
    public String candidateId;

    public String fileName;
    public long sizeBytes;

    /** AES-256-GCM ciphertext (IV + data + tag). */
    public byte[] dataEnc;

    public Instant uploadedAt = Instant.now();
}
