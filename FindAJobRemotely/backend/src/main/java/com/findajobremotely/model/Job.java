package com.findajobremotely.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("jobs")
@CompoundIndex(name = "status_created", def = "{'status': 1, 'createdAt': -1}")
public class Job {

    @Id
    public String id;

    @Indexed
    public String employerId;

    public String companyName;
    public String title;
    public String description;
    public JobType type;
    public ExperienceLevel experienceLevel;
    public List<String> skills = new ArrayList<>();

    /** Monthly pay / stipend in whole currency units. Optional (unpaid internships). */
    public Long salaryMin;
    public Long salaryMax;
    public String currency;

    public String remoteRegion;
    public int openings = 1;
    public int hiredCount;
    public JobStatus status = JobStatus.OPEN;

    public Instant createdAt = Instant.now();
    public Instant updatedAt = Instant.now();
}
