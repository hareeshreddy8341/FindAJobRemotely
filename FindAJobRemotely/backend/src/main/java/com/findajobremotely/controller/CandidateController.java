package com.findajobremotely.controller;

import com.findajobremotely.dto.ApplicationDtos.ApplicationView;
import com.findajobremotely.dto.ApplicationDtos.ApplyRequest;
import com.findajobremotely.dto.ApplicationDtos.CommissionView;
import com.findajobremotely.dto.CommonDtos.MessageResponse;
import com.findajobremotely.dto.CommonDtos.PageResponse;
import com.findajobremotely.dto.JobDtos.JobMatchView;
import com.findajobremotely.dto.PostDtos.PostRequest;
import com.findajobremotely.dto.PostDtos.PostView;
import com.findajobremotely.dto.ProfileDtos.CandidateProfileRequest;
import com.findajobremotely.dto.ProfileDtos.CandidateProfileView;
import com.findajobremotely.security.AuthPrincipal;
import com.findajobremotely.service.ApplicationService;
import com.findajobremotely.service.MatchingService;
import com.findajobremotely.service.PostService;
import com.findajobremotely.service.ProfileService;
import com.findajobremotely.service.ResumeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Everything under /api/candidate is restricted to the CANDIDATE role by SecurityConfig. */
@RestController
@RequestMapping("/api/candidate")
public class CandidateController {

    private final ProfileService profiles;
    private final MatchingService matching;
    private final ApplicationService applications;
    private final PostService posts;
    private final ResumeService resumes;

    public CandidateController(ProfileService profiles, MatchingService matching,
                               ApplicationService applications, PostService posts, ResumeService resumes) {
        this.profiles = profiles;
        this.matching = matching;
        this.applications = applications;
        this.posts = posts;
        this.resumes = resumes;
    }

    // ---- profile ----

    @GetMapping("/profile")
    public CandidateProfileView profile(@AuthenticationPrincipal AuthPrincipal me) {
        return profiles.getCandidate(me.id());
    }

    @PutMapping("/profile")
    public CandidateProfileView updateProfile(@AuthenticationPrincipal AuthPrincipal me,
                                              @Valid @RequestBody CandidateProfileRequest req) {
        return profiles.updateCandidate(me.id(), req);
    }

    // ---- resume ----

    @PostMapping(value = "/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MessageResponse uploadResume(@AuthenticationPrincipal AuthPrincipal me,
                                        @RequestPart("file") MultipartFile file) {
        resumes.upload(me.id(), file);
        return new MessageResponse("Resume uploaded");
    }

    @DeleteMapping("/resume")
    public MessageResponse deleteResume(@AuthenticationPrincipal AuthPrincipal me) {
        resumes.delete(me.id());
        return new MessageResponse("Resume removed");
    }

    @GetMapping("/resume")
    public ResponseEntity<byte[]> downloadResume(@AuthenticationPrincipal AuthPrincipal me) {
        return pdf(resumes.downloadOwn(me.id()));
    }

    // ---- matching ----

    @GetMapping("/recommendations")
    public List<JobMatchView> recommendations(@AuthenticationPrincipal AuthPrincipal me,
                                              @RequestParam(defaultValue = "20") int limit) {
        return matching.recommendJobs(me.id(), Math.max(1, Math.min(limit, 50)));
    }

    // ---- applications ----

    @PostMapping("/jobs/{jobId}/apply")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CREATED)
    public ApplicationView apply(@AuthenticationPrincipal AuthPrincipal me, @PathVariable String jobId,
                                 @Valid @RequestBody(required = false) ApplyRequest req) {
        return applications.apply(me.id(), jobId, req);
    }

    @GetMapping("/applications")
    public PageResponse<ApplicationView> history(@AuthenticationPrincipal AuthPrincipal me,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        return applications.history(me.id(), page, size);
    }

    @PostMapping("/applications/{id}/withdraw")
    public ApplicationView withdraw(@AuthenticationPrincipal AuthPrincipal me, @PathVariable String id) {
        return applications.withdraw(me.id(), id);
    }

    @GetMapping("/commissions")
    public List<CommissionView> commissions(@AuthenticationPrincipal AuthPrincipal me) {
        return applications.myCommissions(me.id());
    }

    // ---- posts ----

    @GetMapping("/posts")
    public List<PostView> myPosts(@AuthenticationPrincipal AuthPrincipal me) {
        return posts.mine(me.id());
    }

    @PostMapping("/posts")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CREATED)
    public PostView createPost(@AuthenticationPrincipal AuthPrincipal me, @Valid @RequestBody PostRequest req) {
        return posts.create(me.id(), req);
    }

    @PutMapping("/posts/{id}")
    public PostView updatePost(@AuthenticationPrincipal AuthPrincipal me, @PathVariable String id,
                               @Valid @RequestBody PostRequest req) {
        return posts.update(me.id(), id, req);
    }

    @DeleteMapping("/posts/{id}")
    public MessageResponse deletePost(@AuthenticationPrincipal AuthPrincipal me, @PathVariable String id) {
        posts.delete(me.id(), id);
        return new MessageResponse("Post deleted");
    }

    static ResponseEntity<byte[]> pdf(ResumeService.Download d) {
        ContentDisposition cd = ContentDisposition.attachment().filename(d.fileName()).build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(d.data());
    }
}
