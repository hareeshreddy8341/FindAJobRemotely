package com.findajobremotely.dto;

import com.findajobremotely.dto.AuthDtos.UserView;
import com.findajobremotely.dto.ProfileDtos.CandidatePublicView;
import com.findajobremotely.dto.ProfileDtos.EmployerPublicView;
import com.findajobremotely.model.AccountStatus;
import com.findajobremotely.model.CommissionStatus;
import com.findajobremotely.model.PostStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record AdminStats(long candidates, long employers, long suspendedUsers, long openJobs,
                             long applications, long hires, long pendingCommissions) {
    }

    /** Admins see account metadata and PUBLIC profile data only - never phone, resume or salary expectation. */
    public record AdminUserDetail(UserView user, CandidatePublicView candidate, EmployerPublicView employer,
                                  long jobs, long applications, long posts) {
    }

    public record SetUserStatusRequest(@NotNull AccountStatus status) {
    }

    public record VerifyEmployerRequest(@NotNull Boolean verified) {
    }

    public record PostStatusRequest(@NotNull PostStatus status) {
    }

    public record CommissionUpdateRequest(@NotNull CommissionStatus status, @Size(max = 500) String notes) {
    }

    public record AuditLogView(String id, String actorEmail, String action, String targetType, String targetId,
                               String details, String ip, Instant createdAt) {
    }
}
