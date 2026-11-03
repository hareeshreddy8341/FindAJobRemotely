package com.findajobremotely.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** A candidate's application to a job. The unique index makes double-applying impossible. */
@Document("applications")
@CompoundIndex(name = "job_candidate_unique", def = "{'jobId': 1, 'candidateId': 1}", unique = true)
public class Application {

    @Id
    public String id;

    @Indexed
    public String jobId;

    @Indexed
    public String candidateId;

    @Indexed
    public String employerId;

    // Snapshots so history stays readable even if the job is later closed or removed
    public String jobTitle;
    public String companyName;

    public ApplicationStatus status = ApplicationStatus.APPLIED;
    public String coverNote;
    public int matchScore;

    public Long agreedMonthlySalary;

    public Instant appliedAt = Instant.now();
    public Instant updatedAt = Instant.now();
}
