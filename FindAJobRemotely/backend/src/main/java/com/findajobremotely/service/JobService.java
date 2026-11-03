package com.findajobremotely.service;

import com.findajobremotely.core.Skills;
import com.findajobremotely.core.TextClean;
import com.findajobremotely.dto.CommonDtos.PageResponse;
import com.findajobremotely.dto.JobDtos.JobRequest;
import com.findajobremotely.dto.JobDtos.JobView;
import com.findajobremotely.exception.ApiException;
import com.findajobremotely.model.EmployerProfile;
import com.findajobremotely.model.ExperienceLevel;
import com.findajobremotely.model.Job;
import com.findajobremotely.model.JobStatus;
import com.findajobremotely.model.JobType;
import com.findajobremotely.repository.EmployerProfileRepository;
import com.findajobremotely.repository.JobRepository;
import com.findajobremotely.security.AuthPrincipal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.data.support.PageableExecutionUtils;

@Service
public class JobService {

    private final JobRepository jobs;
    private final EmployerProfileRepository employers;
    private final MongoTemplate template;
    private final ViewMapper mapper;
    private final AuditService audit;

    public JobService(JobRepository jobs, EmployerProfileRepository employers, MongoTemplate template,
                      ViewMapper mapper, AuditService audit) {
        this.jobs = jobs;
        this.employers = employers;
        this.template = template;
        this.mapper = mapper;
        this.audit = audit;
    }

    // ---------------- public browsing ----------------

    public PageResponse<JobView> search(String q, JobType type, ExperienceLevel level, String skill,
                                        int page, int size) {
        Pageable pageable = Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Criteria> all = new ArrayList<>();
        all.add(Criteria.where("status").is(JobStatus.OPEN));
        if (type != null) {
            all.add(Criteria.where("type").is(type));
        }
        if (level != null) {
            all.add(Criteria.where("experienceLevel").is(level));
        }
        String cleanQ = TextClean.line(q);
        if (cleanQ != null && !cleanQ.isBlank()) {
            String rx = TextClean.escapeRegex(cleanQ.length() > 80 ? cleanQ.substring(0, 80) : cleanQ);
            all.add(new Criteria().orOperator(
                    Criteria.where("title").regex(Pattern.compile(rx, Pattern.CASE_INSENSITIVE)),
                    Criteria.where("companyName").regex(Pattern.compile(rx, Pattern.CASE_INSENSITIVE)),
                    Criteria.where("skills").regex(Pattern.compile(rx, Pattern.CASE_INSENSITIVE))));
        }
        String cleanSkill = TextClean.line(skill);
        if (cleanSkill != null && !cleanSkill.isBlank()) {
            String rx = "^" + TextClean.escapeRegex(cleanSkill.length() > 40 ? cleanSkill.substring(0, 40) : cleanSkill) + "$";
            all.add(Criteria.where("skills").regex(Pattern.compile(rx, Pattern.CASE_INSENSITIVE)));
        }
        Query query = new Query(new Criteria().andOperator(all.toArray(new Criteria[0]))).with(pageable);
        List<Job> found = template.find(query, Job.class);
        Page<Job> result = PageableExecutionUtils.getPage(found, pageable,
                () -> template.count(Query.of(query).limit(-1).skip(-1), Job.class));
        return new PageResponse<>(mapper.jobs(found, false), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    /** Open jobs are public. Closed ones are still visible to their owner and to admins. */
    public JobView get(String id, AuthPrincipal viewer) {
        Job j = jobs.findById(id).orElseThrow(() -> ApiException.notFound("Job not found"));
        boolean owner = viewer != null && viewer.id().equals(j.employerId);
        boolean admin = viewer != null && viewer.role() == com.findajobremotely.model.Role.ADMIN;
        if (j.status != JobStatus.OPEN && !owner && !admin) {
            throw ApiException.notFound("Job not found");
        }
        return mapper.jobs(List.of(j), owner || admin).get(0);
    }

    // ---------------- employer ----------------

    public List<JobView> mine(String employerId) {
        return mapper.jobs(jobs.findByEmployerIdOrderByCreatedAtDesc(employerId), true);
    }

    public JobView create(String employerId, JobRequest req) {
        EmployerProfile profile = employers.findById(employerId)
                .orElseThrow(() -> ApiException.badRequest("Complete your company profile first"));
        if (!mapper.employerComplete(profile)) {
            throw ApiException.badRequest("Complete your company profile (name and description) before posting jobs");
        }
        Job j = new Job();
        j.employerId = employerId;
        j.companyName = profile.companyName;
        apply(j, req);
        jobs.save(j);
        return mapper.jobs(List.of(j), true).get(0);
    }

    public JobView update(String employerId, String jobId, JobRequest req) {
        Job j = owned(employerId, jobId);
        if (j.status == JobStatus.REMOVED) {
            throw ApiException.forbidden("This job was removed by an administrator");
        }
        if (req.openings() < j.hiredCount) {
            throw ApiException.badRequest("Openings cannot be fewer than the " + j.hiredCount + " people already hired");
        }
        apply(j, req);
        j.updatedAt = Instant.now();
        jobs.save(j);
        return mapper.jobs(List.of(j), true).get(0);
    }

    public JobView setStatus(String employerId, String jobId, JobStatus status) {
        if (status == JobStatus.REMOVED) {
            throw ApiException.badRequest("Only an administrator can remove a job");
        }
        Job j = owned(employerId, jobId);
        if (j.status == JobStatus.REMOVED) {
            throw ApiException.forbidden("This job was removed by an administrator");
        }
        j.status = status;
        j.updatedAt = Instant.now();
        jobs.save(j);
        return mapper.jobs(List.of(j), true).get(0);
    }

    public void delete(String employerId, String jobId) {
        Job j = owned(employerId, jobId);
        // Jobs with hires are kept (as closed) so commission records stay explainable
        if (j.hiredCount > 0) {
            throw ApiException.conflict("A job with hires cannot be deleted. Close it instead.");
        }
        jobs.deleteById(j.id);
    }

    /** Employers may only touch their own jobs; anything else looks like it does not exist. */
    public Job owned(String employerId, String jobId) {
        return jobs.findById(jobId).filter(j -> j.employerId.equals(employerId))
                .orElseThrow(() -> ApiException.notFound("Job not found"));
    }

    // ---------------- admin ----------------

    public JobView adminSetStatus(AuthPrincipal admin, String jobId, JobStatus status) {
        Job j = jobs.findById(jobId).orElseThrow(() -> ApiException.notFound("Job not found"));
        j.status = status;
        j.updatedAt = Instant.now();
        jobs.save(j);
        audit.log(admin, "JOB_STATUS_" + status, "JOB", j.id, j.title);
        return mapper.jobs(List.of(j), true).get(0);
    }

    public PageResponse<JobView> adminList(int page, int size) {
        Pageable pageable = Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Job> p = jobs.findAll(pageable);
        return new PageResponse<>(mapper.jobs(p.getContent(), true), p.getNumber(), p.getSize(),
                p.getTotalElements(), p.getTotalPages());
    }

    // ---------------- helpers ----------------

    private void apply(Job j, JobRequest req) {
        String title = TextClean.line(req.title());
        String description = TextClean.block(req.description());
        if (title == null || title.length() < 3) {
            throw ApiException.badRequest("Job title is too short");
        }
        if (description == null || description.length() < 20) {
            throw ApiException.badRequest("Please describe the role in a little more detail");
        }
        List<String> skills = Skills.clean(req.skills(), 30);
        if (skills.isEmpty()) {
            throw ApiException.badRequest("Add at least one required skill");
        }
        if (req.salaryMin() != null && req.salaryMax() != null && req.salaryMin() > req.salaryMax()) {
            throw ApiException.badRequest("Minimum pay cannot be higher than maximum pay");
        }
        boolean hasPay = req.salaryMin() != null || req.salaryMax() != null;
        String currency = req.currency() == null || req.currency().isBlank() ? null : req.currency();
        if (hasPay && currency == null) {
            throw ApiException.badRequest("Select a currency for the pay range");
        }
        j.title = title;
        j.description = description;
        j.type = req.type();
        j.experienceLevel = req.experienceLevel();
        j.skills = skills;
        j.salaryMin = req.salaryMin();
        j.salaryMax = req.salaryMax();
        j.currency = hasPay ? currency : null;
        j.remoteRegion = TextClean.line(req.remoteRegion());
        j.openings = req.openings();
    }
}
