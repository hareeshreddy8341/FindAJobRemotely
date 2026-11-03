package com.findajobremotely.dto;

import com.findajobremotely.model.AccountStatus;
import com.findajobremotely.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 100) String fullName,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 10, max = 64) String password,
            @NotNull Role role,
            @Size(max = 120) String companyName) {
    }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 64) String password) {
    }

    public record ChangePasswordRequest(
            @NotBlank @Size(max = 64) String currentPassword,
            @NotBlank @Size(min = 10, max = 64) String newPassword) {
    }

    public record DeleteAccountRequest(@NotBlank @Size(max = 64) String password) {
    }

    public record UserView(String id, String email, String fullName, Role role, AccountStatus status,
                           Instant createdAt, Instant lastLoginAt) {
    }

    public record AuthResponse(String accessToken, long expiresInSeconds, UserView user) {
    }
}
