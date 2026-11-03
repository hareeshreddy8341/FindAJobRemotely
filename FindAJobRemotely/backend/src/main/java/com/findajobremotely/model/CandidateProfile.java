package com.findajobremotely.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * One profile per candidate (id = the user's id).
 *
 * <p>PUBLIC fields are visible to authenticated employers when {@code publicProfile} is true.
 * PRIVATE fields (phone, salary expectation, resume) are encrypted at rest and only revealed to
 * the owner, or to an employer the candidate has actually applied to.
 */
@Document("candidate_profiles")
public class CandidateProfile {

    @Id
    public String userId;

    // ---- public ----
    public String headline;
    public String about;
    public List<String> skills = new ArrayList<>();
    public ExperienceLevel experienceLevel;
    public Integer yearsOfExperience;
    public List<JobType> preferredJobTypes = new ArrayList<>();
    public List<String> preferredRoles = new ArrayList<>();
    public String education;
    public String country;
    public String timezone;
    public String githubUrl;
    public String linkedinUrl;
    public String portfolioUrl;

    /** Candidate-controlled switch: when false the profile is hidden from talent search. */
    @Indexed
    public boolean publicProfile = true;

    /** Mirrors the account status so suspended users disappear from talent search. */
    @Indexed
    public boolean accountActive = true;

    // ---- private (encrypted at rest) ----
    public String phoneEnc;
    public String expectedMonthlySalaryEnc;
    public String currency;

    // ---- resume metadata (the file itself lives in ResumeFile) ----
    public String resumeFileName;
    public Long resumeSizeBytes;
    public Instant resumeUploadedAt;

    public Instant createdAt = Instant.now();
    public Instant updatedAt = Instant.now();
}
