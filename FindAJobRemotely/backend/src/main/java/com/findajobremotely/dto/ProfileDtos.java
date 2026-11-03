package com.findajobremotely.dto;

import com.findajobremotely.model.ExperienceLevel;
import com.findajobremotely.model.JobType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class ProfileDtos {

    /** Only http(s) links, so a "javascript:" URL can never end up in an href. */
    public static final String URL_REGEX = "^$|^https?://[^\\s<>\"']{3,300}$";
    public static final String PHONE_REGEX = "^$|^[0-9+()\\-\\s]{6,20}$";
    public static final String CURRENCY_REGEX = "^$|^[A-Z]{3}$";

    private ProfileDtos() {
    }

    // ---------------- candidate ----------------

    public record CandidateProfileRequest(
            @Size(min = 2, max = 100) String fullName,
            @Size(max = 120) String headline,
            @Size(max = 3000) String about,
            @Size(max = 30) List<@NotBlank @Size(max = 40) String> skills,
            ExperienceLevel experienceLevel,
            @Min(0) @Max(60) Integer yearsOfExperience,
            @Size(max = 4) List<JobType> preferredJobTypes,
            @Size(max = 10) List<@NotBlank @Size(max = 60) String> preferredRoles,
            @Size(max = 300) String education,
            @Size(max = 60) String country,
            @Size(max = 60) String timezone,
            @Pattern(regexp = URL_REGEX) String githubUrl,
            @Pattern(regexp = URL_REGEX) String linkedinUrl,
            @Pattern(regexp = URL_REGEX) String portfolioUrl,
            Boolean publicProfile,
            @Pattern(regexp = PHONE_REGEX) String phone,
            @Min(0) @Max(100000000) Long expectedMonthlySalary,
            @Pattern(regexp = CURRENCY_REGEX) String currency) {
    }

    /** What the candidate sees about themselves - includes private fields. */
    public record CandidateProfileView(
            String userId, String fullName, String email,
            String headline, String about, List<String> skills, ExperienceLevel experienceLevel,
            Integer yearsOfExperience, List<JobType> preferredJobTypes, List<String> preferredRoles,
            String education, String country, String timezone,
            String githubUrl, String linkedinUrl, String portfolioUrl,
            boolean publicProfile, String phone, Long expectedMonthlySalary, String currency,
            boolean hasResume, String resumeFileName, Long resumeSizeBytes, Instant resumeUploadedAt,
            boolean profileComplete) {
    }

    /** What employers browsing talent see. No email, phone, salary expectation or resume. */
    public record CandidatePublicView(
            String userId, String fullName, String headline, String about, List<String> skills,
            ExperienceLevel experienceLevel, Integer yearsOfExperience,
            List<JobType> preferredJobTypes, List<String> preferredRoles,
            String education, String country, String timezone,
            String githubUrl, String linkedinUrl, String portfolioUrl) {
    }

    /** Revealed to an employer only for candidates who applied to that employer's jobs. */
    public record ApplicantView(CandidatePublicView profile, String email, String phone,
                                boolean hasResume, String resumeFileName) {
    }

    // ---------------- employer ----------------

    public record EmployerProfileRequest(
            @NotBlank @Size(min = 2, max = 120) String companyName,
            @Pattern(regexp = URL_REGEX) String website,
            @Size(max = 80) String industry,
            @Size(max = 40) String companySize,
            @Size(max = 3000) String about,
            @Size(max = 60) String country,
            @Min(1800) @Max(2100) Integer foundedYear,
            @Pattern(regexp = PHONE_REGEX) String contactPhone) {
    }

    public record EmployerProfileView(
            String userId, String email, String companyName, String website, String industry,
            String companySize, String about, String country, Integer foundedYear, boolean verified,
            String contactPhone, boolean profileComplete) {
    }

    public record EmployerPublicView(
            String userId, String companyName, String website, String industry, String companySize,
            String about, String country, Integer foundedYear, boolean verified) {
    }
}
