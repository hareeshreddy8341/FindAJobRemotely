package com.findajobremotely.dto;

import com.findajobremotely.dto.ProfileDtos.CandidatePublicView;
import com.findajobremotely.model.PostStatus;
import com.findajobremotely.model.PostType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class PostDtos {

    private PostDtos() {
    }

    public record PostRequest(
            @NotNull PostType type,
            @NotBlank @Size(min = 3, max = 140) String title,
            @NotBlank @Size(min = 10, max = 5000) String content,
            @Size(max = 8) List<@NotBlank @Size(max = 30) String> tags) {
    }

    public record PostView(
            String id, String authorId, String authorName, String authorHeadline, PostType type,
            String title, String content, List<String> tags, PostStatus status,
            Instant createdAt, Instant updatedAt) {
    }

    public record TalentDetail(CandidatePublicView profile, List<PostView> posts) {
    }
}
