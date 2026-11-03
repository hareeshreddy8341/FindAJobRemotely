package com.findajobremotely.service;

import com.findajobremotely.core.TextClean;
import com.findajobremotely.dto.AdminDtos.AdminStats;
import com.findajobremotely.dto.AdminDtos.AdminUserDetail;
import com.findajobremotely.dto.AdminDtos.AuditLogView;
import com.findajobremotely.dto.ApplicationDtos.CommissionView;
import com.findajobremotely.dto.AuthDtos.UserView;
import com.findajobremotely.dto.CommonDtos.PageResponse;
import com.findajobremotely.exception.ApiException;
import com.findajobremotely.model.AccountStatus;
import com.findajobremotely.model.ApplicationStatus;
import com.findajobremotely.model.AuditLog;
import com.findajobremotely.model.Commission;
import com.findajobremotely.model.CommissionStatus;
import com.findajobremotely.model.EmployerProfile;
import com.findajobremotely.model.JobStatus;
import com.findajobremotely.model.Role;
import com.findajobremotely.model.User;
import com.findajobremotely.repository.ApplicationRepository;
import com.findajobremotely.repository.AuditLogRepository;
import com.findajobremotely.repository.CandidatePostRepository;
import com.findajobremotely.repository.CandidateProfileRepository;
import com.findajobremotely.repository.CommissionRepository;
import com.findajobremotely.repository.EmployerProfileRepository;
import com.findajobremotely.repository.JobRepository;
import com.findajobremotely.repository.UserRepository;
import com.findajobremotely.security.AuthPrincipal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

/**
 * Administrator operations. Admins can see account metadata and PUBLIC profile data; private fields
 * (phone, salary expectation, resumes) are intentionally not exposed here.
 */
@Service
public class AdminService {

    private final UserRepository users;
    private final CandidateProfileRepository candidates;
    private final EmployerProfileRepository employers;
    private final JobRepository jobs;
    private final ApplicationRepository applications;
    private final CandidatePostRepository posts;
    private final CommissionRepository commissions;
    private final AuditLogRepository auditLogs;
    private final AccountService accounts;
    private final AuditService audit;
    private final ViewMapper mapper;
    private final MongoTemplate template;

    public AdminService(UserRepository users, CandidateProfileRepository candidates,
                        EmployerProfileRepository employers, JobRepository jobs,
                        ApplicationRepository applications, CandidatePostRepository posts,
                        CommissionRepository commissions, AuditLogRepository auditLogs,
                        AccountService accounts, AuditService audit, ViewMapper mapper, MongoTemplate template) {
        this.users = users;
        this.candidates = candidates;
        this.employers = employers;
        this.jobs = jobs;
        this.applications = applications;
        this.posts = posts;
        this.commissions = commissions;
        this.auditLogs = auditLogs;
        this.accounts = accounts;
        this.audit = audit;
        this.mapper = mapper;
        this.template = template;
    }

    public AdminStats stats() {
        return new AdminStats(
                users.countByRole(Role.CANDIDATE),
                users.countByRole(Role.EMPLOYER),
                users.countByStatus(AccountStatus.SUSPENDED),
                jobs.countByStatus(JobStatus.OPEN),
                applications.count(),
                applications.countByStatus(ApplicationStatus.HIRED),
                commissions.findByStatus(CommissionStatus.PENDING).size());
    }

    // ---------------- users ----------------

    public PageResponse<UserView> listUsers(String q, Role role, AccountStatus status, int page, int size) {
        Pageable pageable = Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Criteria> all = new ArrayList<>();
        // Admin accounts are not managed through this screen
        all.add(Criteria.where("role").in(Role.CANDIDATE, Role.EMPLOYER));
        if (role == Role.CANDIDATE || role == Role.EMPLOYER) {
            all.add(Criteria.where("role").is(role));
        }
        if (status != null) {
            all.add(Criteria.where("status").is(status));
        }
        String cleanQ = TextClean.line(q);
        if (cleanQ != null && !cleanQ.isBlank()) {
            String rx = TextClean.escapeRegex(cleanQ.length() > 80 ? cleanQ.substring(0, 80) : cleanQ);
            Pattern pattern = Pattern.compile(rx, Pattern.CASE_INSENSITIVE);
            all.add(new Criteria().orOperator(Criteria.where("email").regex(pattern),
                    Criteria.where("fullName").regex(pattern)));
        }
        Query query = new Query(new Criteria().andOperator(all.toArray(new Criteria[0])));
        long total = template.count(query, User.class);
        List<User> found = template.find(query.with(pageable), User.class);
        return PageResponse.of(new PageImpl<>(found.stream().map(mapper::user).toList(), pageable, total));
    }

    public AdminUserDetail userDetail(String userId) {
        User u = managedUser(userId);
        if (u.role == Role.CANDIDATE) {
            var cp = candidates.findById(userId).orElse(null);
            return new AdminUserDetail(mapper.user(u), cp == null ? null : mapper.candidatePublic(u, cp), null,
                    0, applications.countByCandidateId(userId), posts.countByAuthorId(userId));
        }
        EmployerProfile ep = employers.findById(userId).orElse(null);
        long jobCount = jobs.findByEmployerIdOrderByCreatedAtDesc(userId).size();
        return new AdminUserDetail(mapper.user(u), null, ep == null ? null : mapper.employerPublic(ep),
                jobCount, 0, 0);
    }

    public UserView setUserStatus(AuthPrincipal admin, String userId, AccountStatus status) {
        User u = managedUser(userId);
        accounts.setStatus(u, status);
        audit.log(admin, "USER_" + status, "USER", u.id, u.email);
        return mapper.user(u);
    }

    public void deleteUser(AuthPrincipal admin, String userId) {
        User u = managedUser(userId);
        accounts.deleteUserAndData(u);
        audit.log(admin, "USER_DELETED", "USER", u.id, u.email);
    }

    public EmployerProfile verifyEmployer(AuthPrincipal admin, String userId, boolean verified) {
        EmployerProfile p = employers.findById(userId).orElseThrow(() -> ApiException.notFound("Employer not found"));
        p.verified = verified;
        p.updatedAt = Instant.now();
        employers.save(p);
        audit.log(admin, verified ? "EMPLOYER_VERIFIED" : "EMPLOYER_UNVERIFIED", "USER", userId, p.companyName);
        return p;
    }

    /** Admins cannot act on other admins, and the target must exist. */
    private User managedUser(String userId) {
        User u = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        if (u.role == Role.ADMIN) {
            throw ApiException.forbidden("Administrator accounts cannot be managed here");
        }
        return u;
    }

    // ---------------- commissions ----------------

    public PageResponse<CommissionView> listCommissions(CommissionStatus status, int page, int size) {
        Pageable pageable = Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        org.springframework.data.domain.Page<Commission> p = status == null
                ? commissions.findAll(pageable) : commissions.findByStatus(status, pageable);
        return new PageResponse<>(mapper.commissions(p.getContent()), p.getNumber(), p.getSize(),
                p.getTotalElements(), p.getTotalPages());
    }

    public CommissionView updateCommission(AuthPrincipal admin, String id, CommissionStatus status, String notes) {
        Commission c = commissions.findById(id).orElseThrow(() -> ApiException.notFound("Commission not found"));
        c.status = status;
        c.notes = TextClean.line(notes);
        c.paidAt = status == CommissionStatus.PAID ? Instant.now() : null;
        commissions.save(c);
        audit.log(admin, "COMMISSION_" + status, "COMMISSION", c.id, "amount=" + c.amount + " " + c.currency);
        return mapper.commissions(List.of(c)).get(0);
    }

    // ---------------- audit ----------------

    public PageResponse<AuditLogView> auditLog(int page, int size) {
        Pageable pageable = Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        org.springframework.data.domain.Page<AuditLog> p = auditLogs.findAll(pageable);
        return new PageResponse<>(p.getContent().stream()
                .map(a -> new AuditLogView(a.id, a.actorEmail, a.action, a.targetType, a.targetId, a.details, a.ip,
                        a.createdAt)).toList(),
                p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }
}
