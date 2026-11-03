package com.findajobremotely.controller;

import com.findajobremotely.dto.AdminDtos.AdminStats;
import com.findajobremotely.dto.AdminDtos.AdminUserDetail;
import com.findajobremotely.dto.AdminDtos.AuditLogView;
import com.findajobremotely.dto.AdminDtos.CommissionUpdateRequest;
import com.findajobremotely.dto.AdminDtos.PostStatusRequest;
import com.findajobremotely.dto.AdminDtos.SetUserStatusRequest;
import com.findajobremotely.dto.AdminDtos.VerifyEmployerRequest;
import com.findajobremotely.dto.ApplicationDtos.CommissionView;
import com.findajobremotely.dto.AuthDtos.UserView;
import com.findajobremotely.dto.CommonDtos.MessageResponse;
import com.findajobremotely.dto.CommonDtos.PageResponse;
import com.findajobremotely.dto.JobDtos.JobStatusRequest;
import com.findajobremotely.dto.JobDtos.JobView;
import com.findajobremotely.dto.PostDtos.PostView;
import com.findajobremotely.dto.ProfileDtos.EmployerPublicView;
import com.findajobremotely.model.AccountStatus;
import com.findajobremotely.model.CommissionStatus;
import com.findajobremotely.model.Role;
import com.findajobremotely.security.AuthPrincipal;
import com.findajobremotely.service.AdminService;
import com.findajobremotely.service.JobService;
import com.findajobremotely.service.PostService;
import com.findajobremotely.service.ViewMapper;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Everything under /api/admin is restricted to the ADMIN role by SecurityConfig. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService admin;
    private final JobService jobs;
    private final PostService posts;
    private final ViewMapper mapper;

    public AdminController(AdminService admin, JobService jobs, PostService posts, ViewMapper mapper) {
        this.admin = admin;
        this.jobs = jobs;
        this.posts = posts;
        this.mapper = mapper;
    }

    @GetMapping("/stats")
    public AdminStats stats() {
        return admin.stats();
    }

    // ---- users ----

    @GetMapping("/users")
    public PageResponse<UserView> users(@RequestParam(required = false) String q,
                                        @RequestParam(required = false) Role role,
                                        @RequestParam(required = false) AccountStatus status,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "15") int size) {
        return admin.listUsers(q, role, status, page, size);
    }

    @GetMapping("/users/{id}")
    public AdminUserDetail user(@PathVariable String id) {
        return admin.userDetail(id);
    }

    @PatchMapping("/users/{id}/status")
    public UserView setStatus(@AuthenticationPrincipal AuthPrincipal me, @PathVariable String id,
                              @Valid @RequestBody SetUserStatusRequest req) {
        return admin.setUserStatus(me, id, req.status());
    }

    @DeleteMapping("/users/{id}")
    public MessageResponse delete(@AuthenticationPrincipal AuthPrincipal me, @PathVariable String id) {
        admin.deleteUser(me, id);
        return new MessageResponse("User and all their data were deleted");
    }

    @PatchMapping("/employers/{id}/verify")
    public EmployerPublicView verify(@AuthenticationPrincipal AuthPrincipal me, @PathVariable String id,
                                     @Valid @RequestBody VerifyEmployerRequest req) {
        return mapper.employerPublic(admin.verifyEmployer(me, id, req.verified()));
    }

    // ---- jobs ----

    @GetMapping("/jobs")
    public PageResponse<JobView> jobs(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "15") int size) {
        return jobs.adminList(page, size);
    }

    @PatchMapping("/jobs/{id}/status")
    public JobView setJobStatus(@AuthenticationPrincipal AuthPrincipal me, @PathVariable String id,
                                @Valid @RequestBody JobStatusRequest req) {
        return jobs.adminSetStatus(me, id, req.status());
    }

    // ---- posts ----

    @GetMapping("/posts")
    public PageResponse<PostView> posts(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "15") int size) {
        return posts.adminList(page, size);
    }

    @PatchMapping("/posts/{id}/status")
    public PostView setPostStatus(@AuthenticationPrincipal AuthPrincipal me, @PathVariable String id,
                                  @Valid @RequestBody PostStatusRequest req) {
        return posts.adminSetStatus(me, id, req.status());
    }

    // ---- commissions & audit ----

    @GetMapping("/commissions")
    public PageResponse<CommissionView> commissions(@RequestParam(required = false) CommissionStatus status,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "15") int size) {
        return admin.listCommissions(status, page, size);
    }

    @PatchMapping("/commissions/{id}")
    public CommissionView updateCommission(@AuthenticationPrincipal AuthPrincipal me, @PathVariable String id,
                                           @Valid @RequestBody CommissionUpdateRequest req) {
        return admin.updateCommission(me, id, req.status(), req.notes());
    }

    @GetMapping("/audit")
    public PageResponse<AuditLogView> audit(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "25") int size) {
        return admin.auditLog(page, size);
    }
}
