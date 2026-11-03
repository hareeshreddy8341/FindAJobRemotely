package com.findajobremotely.service;

import com.findajobremotely.config.AppProperties;
import com.findajobremotely.core.ApplicationFlow;
import com.findajobremotely.core.CommissionCalculator;
import com.findajobremotely.core.MatchingEngine.MatchResult;
import com.findajobremotely.core.TextClean;
import com.findajobremotely.dto.ApplicationDtos.ApplicationView;
import com.findajobremotely.dto.ApplicationDtos.ApplyRequest;
import com.findajobremotely.dto.ApplicationDtos.CommissionView;
import com.findajobremotely.dto.ApplicationDtos.EmployerApplicationView;
import com.findajobremotely.dto.ApplicationDtos.UpdateApplicationStatusRequest;
import com.findajobremotely.dto.CommonDtos.PageResponse;
import com.findajobremotely.exception.ApiException;
import com.findajobremotely.model.Application;
import com.findajobremotely.model.ApplicationStatus;
import com.findajobremotely.model.CandidateProfile;
import com.findajobremotely.model.Commission;
import com.findajobremotely.model.Job;
import com.findajobremotely.model.JobStatus;
import com.findajobremotely.model.User;
import com.findajobremotely.repository.ApplicationRepository;
import com.findajobremotely.repository.CandidateProfileRepository;
import com.findajobremotely.repository.CommissionRepository;
import com.findajobremotely.repository.JobRepository;
import com.findajobremotely.repository.UserRepository;
import com.findajobremotely.security.AuthPrincipal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class ApplicationService {

    private final ApplicationRepository applications;
    private final JobRepository jobs;
    private final CandidateProfileRepository candidates;
    private final UserRepository users;
    private final CommissionRepository commissions;
    private final MatchingService matching;
    private final JobService jobService;
    private final ViewMapper mapper;
    private final AuditService audit;
    private final AppProperties props;

    public ApplicationService(ApplicationRepository applications, JobRepository jobs,
                              CandidateProfileRepository candidates, UserRepository users,
                              CommissionRepository commissions, MatchingService matching, JobService jobService,
                              ViewMapper mapper, AuditService audit, AppProperties props) {
        this.applications = applications;
        this.jobs = jobs;
        this.candidates = candidates;
        this.users = users;
        this.commissions = commissions;
        this.matching = matching;
        this.jobService = jobService;
        this.mapper = mapper;
        this.audit = audit;
        this.props = props;
    }

    // ---------------- candidate ----------------

    public ApplicationView apply(String candidateId, String jobId, ApplyRequest req) {
        Job job = jobs.findById(jobId).filter(j -> j.status == JobStatus.OPEN)
                .orElseThrow(() -> ApiException.notFound("This job is no longer open"));
        CandidateProfile profile = candidates.findById(candidateId)
                .orElseThrow(() -> ApiException.badRequest("Complete your profile before applying"));
        if (!mapper.candidateComplete(profile)) {
            throw ApiException.badRequest("Add a headline and at least one skill to your profile before applying");
        }
        if (applications.findByJobIdAndCandidateId(jobId, candidateId).isPresent()) {
            throw ApiException.conflict("You have already applied to this job");
        }
        MatchResult match = matching.score(profile, job);

        Application a = new Application();
        a.jobId = job.id;
        a.candidateId = candidateId;
        a.employerId = job.employerId;
        a.jobTitle = job.title;
        a.companyName = job.companyName;
        a.coverNote = req == null ? null : blankToNull(TextClean.block(req.coverNote()));
        a.matchScore = match.score();
        try {
            applications.save(a);
        } catch (DuplicateKeyException e) {
            throw ApiException.conflict("You have already applied to this job");
        }
        return toCandidateView(List.of(a)).get(0);
    }

    public PageResponse<ApplicationView> history(String candidateId, int page, int size) {
        Pageable pageable = Paging.of(page, size, Sort.by(Sort.Direction.DESC, "appliedAt"));
        Page<Application> p = applications.findByCandidateId(candidateId, pageable);
        return new PageResponse<>(toCandidateView(p.getContent()), p.getNumber(), p.getSize(),
                p.getTotalElements(), p.getTotalPages());
    }

    public ApplicationView withdraw(String candidateId, String applicationId) {
        Application a = applications.findById(applicationId).filter(x -> x.candidateId.equals(candidateId))
                .orElseThrow(() -> ApiException.notFound("Application not found"));
        if (!ApplicationFlow.candidateMayWithdraw(a.status)) {
            throw ApiException.conflict("This application can no longer be withdrawn");
        }
        a.status = ApplicationStatus.WITHDRAWN;
        a.updatedAt = Instant.now();
        applications.save(a);
        return toCandidateView(List.of(a)).get(0);
    }

    public List<CommissionView> myCommissions(String candidateId) {
        return mapper.commissions(commissions.findByCandidateIdOrderByCreatedAtDesc(candidateId));
    }

    // ---------------- employer ----------------

    public PageResponse<EmployerApplicationView> forJob(String employerId, String jobId, ApplicationStatus status,
                                                        int page, int size) {
        Job job = jobService.owned(employerId, jobId);
        Pageable pageable = Paging.of(page, size, Sort.by(Sort.Direction.DESC, "matchScore", "appliedAt"));
        Page<Application> p = status == null
                ? applications.findByJobId(job.id, pageable)
                : applications.findByJobIdAndStatus(job.id, status, pageable);

        List<Application> list = p.getContent();
        Map<String, User> usersById = mapper.usersById(list.stream().map(a -> a.candidateId).toList());
        Map<String, CandidateProfile> profiles =
                mapper.candidateProfilesById(list.stream().map(a -> a.candidateId).toList());

        List<EmployerApplicationView> views = list.stream()
                .filter(a -> profiles.containsKey(a.candidateId))
                .map(a -> new EmployerApplicationView(a.id, a.jobId, a.jobTitle, a.status, a.coverNote,
                        a.matchScore, a.appliedAt, a.updatedAt, a.agreedMonthlySalary,
                        mapper.applicant(usersById.get(a.candidateId), profiles.get(a.candidateId))))
                .toList();
        return new PageResponse<>(views, p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    public EmployerApplicationView updateStatus(AuthPrincipal employer, String applicationId,
                                                UpdateApplicationStatusRequest req) {
        Application a = applications.findById(applicationId).filter(x -> x.employerId.equals(employer.id()))
                .orElseThrow(() -> ApiException.notFound("Application not found"));
        if (!ApplicationFlow.employerMayMove(a.status, req.status())) {
            throw ApiException.conflict("This application cannot move from " + a.status + " to " + req.status());
        }
        Job job = jobs.findById(a.jobId).orElseThrow(() -> ApiException.notFound("Job not found"));

        if (req.status() == ApplicationStatus.HIRED) {
            recordHire(employer, a, job, req);
        } else {
            a.status = req.status();
            a.updatedAt = Instant.now();
            applications.save(a);
        }
        User u = users.findById(a.candidateId).orElse(null);
        CandidateProfile p = candidates.findById(a.candidateId).orElse(null);
        if (p == null) {
            throw ApiException.notFound("Candidate not found");
        }
        return new EmployerApplicationView(a.id, a.jobId, a.jobTitle, a.status, a.coverNote, a.matchScore,
                a.appliedAt, a.updatedAt, a.agreedMonthlySalary, mapper.applicant(u, p));
    }

    /** Marks the hire, bumps the job's hire counter, closes the job when full and creates the commission. */
    private void recordHire(AuthPrincipal employer, Application a, Job job, UpdateApplicationStatusRequest req) {
        if (req.agreedMonthlySalary() == null) {
            throw ApiException.badRequest("Enter the agreed monthly pay to record the hire");
        }
        String currency = req.currency() != null && !req.currency().isBlank() ? req.currency() : job.currency;
        if (currency == null) {
            throw ApiException.badRequest("Select the currency of the agreed pay");
        }
        if (job.hiredCount >= job.openings) {
            throw ApiException.conflict("All openings for this job are already filled");
        }
        long amount = CommissionCalculator.amount(req.agreedMonthlySalary(), props.commission().percent());

        a.status = ApplicationStatus.HIRED;
        a.agreedMonthlySalary = req.agreedMonthlySalary();
        a.updatedAt = Instant.now();
        applications.save(a);

        job.hiredCount += 1;
        if (job.hiredCount >= job.openings) {
            job.status = JobStatus.CLOSED;
        }
        job.updatedAt = Instant.now();
        jobs.save(job);

        Commission c = new Commission();
        c.applicationId = a.id;
        c.jobId = job.id;
        c.jobTitle = job.title;
        c.companyName = job.companyName;
        c.candidateId = a.candidateId;
        c.employerId = a.employerId;
        c.agreedMonthlySalary = req.agreedMonthlySalary();
        c.currency = currency;
        c.percent = props.commission().percent().doubleValue();
        c.amount = amount;
        try {
            commissions.save(c);
        } catch (DuplicateKeyException ignored) {
            // already recorded - the unique index on applicationId makes this safe to repeat
        }
        audit.log(employer, "HIRE_RECORDED", "APPLICATION", a.id, "job=" + job.id + " amount=" + amount + " " + currency);
    }

    // ---------------- mapping ----------------

    private List<ApplicationView> toCandidateView(List<Application> list) {
        Map<String, Commission> byApp = list.isEmpty() ? Collections.emptyMap()
                : commissions.findByApplicationIdIn(list.stream().map(a -> a.id).toList()).stream()
                        .collect(Collectors.toMap(c -> c.applicationId, Function.identity(), (x, y) -> x));
        return list.stream().map(a -> {
            Commission c = byApp.get(a.id);
            CommissionView cv = c == null ? null : mapper.commissions(List.of(c)).get(0);
            return new ApplicationView(a.id, a.jobId, a.jobTitle, a.companyName, a.status, a.coverNote,
                    a.matchScore, a.appliedAt, a.updatedAt, a.agreedMonthlySalary, cv);
        }).toList();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
