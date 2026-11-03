package com.findajobremotely.service;

import com.findajobremotely.core.MatchingEngine;
import com.findajobremotely.core.MatchingEngine.CandidateSignals;
import com.findajobremotely.core.MatchingEngine.JobSignals;
import com.findajobremotely.core.MatchingEngine.MatchResult;
import com.findajobremotely.core.Skills;
import com.findajobremotely.dto.JobDtos.CandidateMatchView;
import com.findajobremotely.dto.JobDtos.JobMatchView;
import com.findajobremotely.dto.JobDtos.JobView;
import com.findajobremotely.exception.ApiException;
import com.findajobremotely.model.Application;
import com.findajobremotely.model.CandidateProfile;
import com.findajobremotely.model.Job;
import com.findajobremotely.model.JobStatus;
import com.findajobremotely.repository.ApplicationRepository;
import com.findajobremotely.repository.CandidateProfileRepository;
import com.findajobremotely.repository.JobRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/** Finds the best jobs for a candidate and the best candidates for a job, using {@link MatchingEngine}. */
@Service
public class MatchingService {

    private static final int POOL_SIZE = 500;

    private final JobRepository jobs;
    private final CandidateProfileRepository candidates;
    private final ApplicationRepository applications;
    private final ViewMapper mapper;

    public MatchingService(JobRepository jobs, CandidateProfileRepository candidates,
                           ApplicationRepository applications, ViewMapper mapper) {
        this.jobs = jobs;
        this.candidates = candidates;
        this.applications = applications;
        this.mapper = mapper;
    }

    public CandidateSignals signals(CandidateProfile p) {
        return new CandidateSignals(
                Skills.normalizeAll(p.skills),
                p.preferredJobTypes == null ? Set.of() : new LinkedHashSet<>(p.preferredJobTypes),
                p.experienceLevel,
                p.preferredRoles == null ? List.of() : p.preferredRoles,
                mapper.expectedSalary(p),
                p.currency);
    }

    public JobSignals signals(Job j) {
        return new JobSignals(Skills.normalizeAll(j.skills), j.type, j.experienceLevel, j.title, j.salaryMax,
                j.currency);
    }

    public MatchResult score(CandidateProfile p, Job j) {
        return MatchingEngine.score(signals(p), signals(j));
    }

    /** Open jobs ranked for one candidate, excluding jobs they already applied to. */
    public List<JobMatchView> recommendJobs(String candidateId, int limit) {
        CandidateProfile profile = candidates.findById(candidateId)
                .orElseThrow(() -> ApiException.notFound("Profile not found"));
        CandidateSignals cs = signals(profile);

        Set<String> applied = new HashSet<>();
        for (Application a : applications.findByCandidateId(candidateId)) {
            applied.add(a.jobId);
        }
        List<Job> pool = jobs.findByStatus(JobStatus.OPEN,
                PageRequest.of(0, POOL_SIZE, Sort.by(Sort.Direction.DESC, "createdAt")));

        record Scored(Job job, MatchResult result) {
        }
        List<Scored> scored = new ArrayList<>();
        for (Job j : pool) {
            if (!applied.contains(j.id)) {
                scored.add(new Scored(j, MatchingEngine.score(cs, signals(j))));
            }
        }
        scored.sort(Comparator.comparingInt((Scored s) -> s.result().score()).reversed()
                .thenComparing(s -> s.job().createdAt, Comparator.reverseOrder()));

        List<Scored> top = scored.subList(0, Math.min(limit, scored.size()));
        List<JobView> views = mapper.jobs(top.stream().map(Scored::job).toList(), false);
        List<JobMatchView> out = new ArrayList<>();
        for (int i = 0; i < top.size(); i++) {
            MatchResult r = top.get(i).result();
            out.add(new JobMatchView(views.get(i), r.score(), r.matchedSkills(), r.missingSkills()));
        }
        return out;
    }

    /** Public candidate profiles ranked for one of the employer's own jobs. */
    public List<CandidateMatchView> candidatesForJob(String employerId, String jobId, int limit) {
        Job job = jobs.findById(jobId).filter(j -> j.employerId.equals(employerId))
                .orElseThrow(() -> ApiException.notFound("Job not found"));
        JobSignals js = signals(job);

        Page<CandidateProfile> pool = candidates.findByPublicProfileTrueAndAccountActiveTrue(
                PageRequest.of(0, POOL_SIZE, Sort.by(Sort.Direction.DESC, "updatedAt")));

        record Scored(CandidateProfile profile, MatchResult result) {
        }
        List<Scored> scored = new ArrayList<>();
        for (CandidateProfile p : pool.getContent()) {
            if (mapper.candidateComplete(p)) {
                scored.add(new Scored(p, MatchingEngine.score(signals(p), js)));
            }
        }
        scored.sort(Comparator.comparingInt((Scored s) -> s.result().score()).reversed());

        List<Scored> top = scored.subList(0, Math.min(limit, scored.size()));
        List<com.findajobremotely.dto.ProfileDtos.CandidatePublicView> views =
                mapper.candidatesPublic(top.stream().map(Scored::profile).toList());
        List<CandidateMatchView> out = new ArrayList<>();
        for (int i = 0; i < top.size(); i++) {
            MatchResult r = top.get(i).result();
            out.add(new CandidateMatchView(views.get(i), r.score(), r.matchedSkills(), r.missingSkills()));
        }
        return out;
    }
}
