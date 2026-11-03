package com.findajobremotely.dto;

import com.findajobremotely.dto.ProfileDtos.ApplicantView;
import com.findajobremotely.model.ApplicationStatus;
import com.findajobremotely.model.CommissionStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class ApplicationDtos {

    private ApplicationDtos() {
    }

    public record ApplyRequest(@Size(max = 2000) String coverNote) {
    }

    public record UpdateApplicationStatusRequest(
            @NotNull ApplicationStatus status,
            @Min(1) @Max(100000000) Long agreedMonthlySalary,
            @Pattern(regexp = ProfileDtos.CURRENCY_REGEX) String currency) {
    }

    public record CommissionView(
            String id, String applicationId, String jobId, String jobTitle, String companyName,
            String candidateId, String candidateName, long agreedMonthlySalary, String currency,
            double percent, long amount, CommissionStatus status, String notes,
            Instant createdAt, Instant paidAt) {
    }

    /** The candidate's own history entry. commission is set once the candidate has been hired. */
    public record ApplicationView(
            String id, String jobId, String jobTitle, String companyName, ApplicationStatus status,
            String coverNote, int matchScore, Instant appliedAt, Instant updatedAt,
            Long agreedMonthlySalary, CommissionView commission) {
    }

    /** What an employer sees for each applicant to their job. */
    public record EmployerApplicationView(
            String id, String jobId, String jobTitle, ApplicationStatus status, String coverNote,
            int matchScore, Instant appliedAt, Instant updatedAt, Long agreedMonthlySalary,
            ApplicantView applicant) {
    }
}
