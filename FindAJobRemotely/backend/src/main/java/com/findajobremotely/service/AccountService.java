package com.findajobremotely.service;

import com.findajobremotely.model.AccountStatus;
import com.findajobremotely.model.CandidateProfile;
import com.findajobremotely.model.Job;
import com.findajobremotely.model.JobStatus;
import com.findajobremotely.model.Role;
import com.findajobremotely.model.User;
import com.findajobremotely.repository.ApplicationRepository;
import com.findajobremotely.repository.CandidatePostRepository;
import com.findajobremotely.repository.CandidateProfileRepository;
import com.findajobremotely.repository.EmployerProfileRepository;
import com.findajobremotely.repository.JobRepository;
import com.findajobremotely.repository.RefreshTokenRepository;
import com.findajobremotely.repository.ResumeFileRepository;
import com.findajobremotely.repository.UserRepository;
import java.time.Instant;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/** Account-level operations that touch many collections: suspension and full erasure. */
@Service
public class AccountService {

    private final UserRepository users;
    private final CandidateProfileRepository candidateProfiles;
    private final EmployerProfileRepository employerProfiles;
    private final JobRepository jobs;
    private final ApplicationRepository applications;
    private final CandidatePostRepository posts;
    private final ResumeFileRepository resumes;
    private final RefreshTokenRepository refreshTokens;
    private final MongoTemplate template;

    public AccountService(UserRepository users, CandidateProfileRepository candidateProfiles,
                          EmployerProfileRepository employerProfiles, JobRepository jobs,
                          ApplicationRepository applications, CandidatePostRepository posts,
                          ResumeFileRepository resumes, RefreshTokenRepository refreshTokens,
                          MongoTemplate template) {
        this.users = users;
        this.candidateProfiles = candidateProfiles;
        this.employerProfiles = employerProfiles;
        this.jobs = jobs;
        this.applications = applications;
        this.posts = posts;
        this.resumes = resumes;
        this.refreshTokens = refreshTokens;
        this.template = template;
    }

    /** Removes the account and everything the user uploaded. Commission records are kept for accounting. */
    public void deleteUserAndData(User user) {
        if (user.role == Role.CANDIDATE) {
            applications.deleteByCandidateId(user.id);
            posts.deleteByAuthorId(user.id);
            resumes.deleteById(user.id);
            candidateProfiles.deleteById(user.id);
        } else if (user.role == Role.EMPLOYER) {
            applications.deleteByEmployerId(user.id);
            jobs.deleteByEmployerId(user.id);
            employerProfiles.deleteById(user.id);
        }
        refreshTokens.deleteByUserId(user.id);
        users.deleteById(user.id);
    }

    /** Suspending signs the user out everywhere, hides their profile and closes an employer's open jobs. */
    public void setStatus(User user, AccountStatus status) {
        user.status = status;
        user.updatedAt = Instant.now();
        users.save(user);

        if (user.role == Role.CANDIDATE) {
            template.updateFirst(Query.query(Criteria.where("id").is(user.id)),
                    Update.update("accountActive", status == AccountStatus.ACTIVE), CandidateProfile.class);
        }
        if (status == AccountStatus.SUSPENDED) {
            refreshTokens.deleteByUserId(user.id);
            if (user.role == Role.EMPLOYER) {
                template.updateMulti(
                        Query.query(Criteria.where("employerId").is(user.id).and("status").is(JobStatus.OPEN.name())),
                        Update.update("status", JobStatus.CLOSED.name()), Job.class);
            }
        }
    }
}
