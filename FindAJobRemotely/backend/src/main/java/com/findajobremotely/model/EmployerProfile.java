package com.findajobremotely.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** One profile per employer (id = the user's id). */
@Document("employer_profiles")
public class EmployerProfile {

    @Id
    public String userId;

    // ---- public ----
    public String companyName;
    public String website;
    public String industry;
    public String companySize;
    public String about;
    public String country;
    public Integer foundedYear;

    /** Set by an administrator only. */
    public boolean verified;

    // ---- private (encrypted at rest) ----
    public String contactPhoneEnc;

    public Instant createdAt = Instant.now();
    public Instant updatedAt = Instant.now();
}
