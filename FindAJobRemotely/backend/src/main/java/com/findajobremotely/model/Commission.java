package com.findajobremotely.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** Created when an employer records a hire. Owed by the candidate; settled manually by an admin in the MVP. */
@Document("commissions")
public class Commission {

    @Id
    public String id;

    @Indexed(unique = true)
    public String applicationId;

    public String jobId;
    public String jobTitle;
    public String companyName;

    @Indexed
    public String candidateId;

    public String employerId;

    public long agreedMonthlySalary;
    public String currency;
    public double percent;
    public long amount;

    public CommissionStatus status = CommissionStatus.PENDING;
    public String notes;

    public Instant createdAt = Instant.now();
    public Instant paidAt;
}
