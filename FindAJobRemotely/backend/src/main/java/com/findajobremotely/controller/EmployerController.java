package com.findajobremotely.controller;

import com.findajobremotely.dto.ApplicationDtos.EmployerApplicationView;
import com.findajobremotely.dto.ApplicationDtos.UpdateApplicationStatusRequest;
import com.findajobremotely.dto.CommonDtos.MessageResponse;
import com.findajobremotely.dto.CommonDtos.PageResponse;
import com.findajobremotely.dto.JobDtos.CandidateMatchView;
import com.findajobremotely.dto.JobDtos.JobRequest;
import com.findajobremotely.dto.JobDtos.JobStatusRequest;
import com.findajobremotely.dto.JobDtos.JobView;
import com.findajobremotely.dto.PostDtos.PostView;
import com.findajobremotely.dto.PostDtos.TalentDetail;
import com.findajobremotely.dto.ProfileDtos.CandidatePublicView;
import com.findajobremotely.dto.ProfileDtos.EmployerProfileRequest;
import com.findajobremotely.dto.ProfileDtos.EmployerProfileView;
import com.findajobremotely.model.ApplicationStatus;
import com.findajobremotely.model.ExperienceLevel;
import com.findajobremotely.model.JobType;
import com.findajobremotely.security.AuthPrincipal;
import com.findajobremotely.service.ApplicationService;
import com.findajobremotely.service.JobService;
import com.findajobremotely.service.MatchingService;
import com.findajobremotely.service.PostService;
import com.findajobremotely.service.ProfileService;
import com.findajobremotely.service.ResumeService;
import com.findajobremotely.service.TalentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Everything under /api/employer is restricted to the EMPLOYER role by SecurityConfig. */
@RestController
@RequestMapping("/api/employer")
public class EmployerController {

    private final ProfileService profiles;
    private final JobService jobs;
    private final ApplicationService applications;
    private final TalentService talent;
    private final PostService posts;
    private final MatchingService matching;
    private final ResumeService resumes;

    public EmployerController(ProfileService profiles, JobService jobs, ApplicationService applications,
                              TalentService talent, PostService posts, MatchingService matching,
                              ResumeService resumes) {
        this.profiles = profiles;
        this.jobs = jobs;
        this.applications = applications;
        this.talent = talent;
        this.posts = posts;
        this.matching = matching;
        this.resumes = resumes;
    }

    // ---- company profile ----

    @GetMapping("/profile")
    public EmployerProfileView profile(@AuthenticationPrincipal AuthPrincipal me) {
        return profiles.getEmployer(me.id());
    }

    @PutMapping("/profile")
    public EmployerProfileView updateProfile(@AuthenticationPrincipal AuthPrincipal me,
                                             @Valid @RequestBody EmployerProfileRequest req) {
        return profiles.updateEmployer(me.id(), req);
    }

    // ---- jobs ----

    @GetMapping("/jobs")
    public List<JobView> myJobs(@AuthenticationPrincipal AuthPrincipal me) {
        return jobs.mine(me.id());
    }

    @PostMapping("/jobs")
    @ResponseStatus(HttpStatus.CREATED)
    public JobView createJob(@AuthenticationPrincipal AuthPrincipal me, @Valid @RequestBody JobRequest req) {
        return jobs.create(me.id(), req);
    }

    @PutMapping("/jobs/{id}")
    public JobView updateJob(@AuthenticationPrincipal AuthPrincipal me, @PathVariable String id,
                             @Valid @RequestBody JobRequest req) {
        return jobs.update(me.id(), id, req);
    }

    @PatchMapping("/jobs/{id}/status")
    public JobView setJobStatus(@AuthenticationPrincipal AuthPrincipal me, @PathVariable String id,
                                @Valid @RequestBody JobStatusRequest req) {
        return jobs.setStatus(me.id(), id, req.status());
    }

    @DeleteMapping("/jobs/{id}")
    public MessageResponse deleteJob(@AuthenticationPrincipal AuthPrincipal me, @PathVariable String id) {
        jobs.delete(me.id(), id);
        return new MessageResponse("Job deleted");
    }

    // ---- applicants ----

    @GetMapping("/jobs/{id}/applications")
    public PageResponse<EmployerApplicationView> applicants(@AuthenticationPrincipal AuthPrincipal me,
                                                            @PathVariable String id,
                                                            @RequestParam(required = false) ApplicationStatus status,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        return applications.forJob(me.id(), id, status, page, size);
    }

    @PatchMapping("/applications/{id}/status")
    public EmployerApplicationView updateApplication(@AuthenticationPrincipal AuthPrincipal me,
                                                     @PathVariable String id,
                                                     @Valid @RequestBody UpdateApplicationStatusRequest req) {
        return applications.updateStatus(me, id, req);
    }

    @GetMapping("/candidates/{candidateId}/resume")
    public ResponseEntity<byte[]> applicantResume(@AuthenticationPrincipal AuthPrincipal me,
                                                  @PathVariable String candidateId) {
        return CandidateController.pdf(resumes.downloadForEmployer(me, candidateId));
    }

    // ---- talent ----

    @GetMapping("/talent")
    public PageResponse<CandidatePublicView> findTalent(@RequestParam(required = false) String q,
                                                        @RequestParam(required = false) String skill,
                                                        @RequestParam(required = false) ExperienceLevel level,
                                                        @RequestParam(required = false) JobType type,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "12") int size) {
        return talent.search(q, skill, level, type, page, size);
    }

    @GetMapping("/talent/{candidateId}")
    public TalentDetail talentDetail(@AuthenticationPrincipal AuthPrincipal me, @PathVariable String candidateId) {
        return talent.detail(me.id(), candidateId);
    }

    @GetMapping("/jobs/{id}/matches")
    public List<CandidateMatchView> matches(@AuthenticationPrincipal AuthPrincipal me, @PathVariable String id,
                                            @RequestParam(defaultValue = "20") int limit) {
        return matching.candidatesForJob(me.id(), id, Math.max(1, Math.min(limit, 50)));
    }

    // ---- candidate posts ----

    @GetMapping("/posts")
    public PageResponse<PostView> candidatePosts(@RequestParam(required = false) String q,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        return posts.feed(q, page, size);
    }
}
