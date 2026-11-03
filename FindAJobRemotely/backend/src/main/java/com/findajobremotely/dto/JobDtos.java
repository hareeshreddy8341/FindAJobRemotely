package com.findajobremotely.dto;

import com.findajobremotely.dto.ProfileDtos.CandidatePublicView;
import com.findajobremotely.model.ExperienceLevel;
import com.findajobremotely.model.JobStatus;
import com.findajobremotely.model.JobType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class JobDtos {

    private JobDtos() {
    }

    public record JobRequest(
            @NotBlank @Size(min = 3, max = 120) String title,
            @NotBlank @Size(min = 20, max = 8000) String description,
            @NotNull JobType type,
            @NotNull ExperienceLevel experienceLevel,
            @NotNull @Size(min = 1, max = 30) List<@NotBlank @Size(max = 40) String> skills,
            @Min(0) @Max(100000000) Long salaryMin,
            @Min(0) @Max(100000000) Long salaryMax,
            @Pattern(regexp = ProfileDtos.CURRENCY_REGEX) String currency,
            @NotBlank @Size(max = 80) String remoteRegion,
            @NotNull @Min(1) @Max(500) Integer openings) {
    }

    public record JobStatusRequest(@NotNull JobStatus status) {
    }

    /** applicantCount is only filled in for the owning employer and admins. */
    public record JobView(
            String id, String employerId, String companyName, boolean companyVerified,
            String title, String description, JobType type, ExperienceLevel experienceLevel,
            List<String> skills, Long salaryMin, Long salaryMax, String currency, String remoteRegion,
            int openings, int hiredCount, JobStatus status, Instant createdAt, Instant updatedAt,
            Long applicantCount) {
    }

    public record JobMatchView(JobView job, int score, List<String> matchedSkills, List<String> missingSkills) {
    }

    public record CandidateMatchView(CandidatePublicView candidate, int score, List<String> matchedSkills,
                                     List<String> missingSkills) {
    }
}
