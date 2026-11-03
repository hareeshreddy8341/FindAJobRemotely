package com.findajobremotely.service;

import com.findajobremotely.core.FieldCrypto;
import com.findajobremotely.dto.ApplicationDtos.CommissionView;
import com.findajobremotely.dto.AuthDtos.UserView;
import com.findajobremotely.dto.JobDtos.JobView;
import com.findajobremotely.dto.PostDtos.PostView;
import com.findajobremotely.dto.ProfileDtos.ApplicantView;
import com.findajobremotely.dto.ProfileDtos.CandidateProfileView;
import com.findajobremotely.dto.ProfileDtos.CandidatePublicView;
import com.findajobremotely.dto.ProfileDtos.EmployerProfileView;
import com.findajobremotely.dto.ProfileDtos.EmployerPublicView;
import com.findajobremotely.model.CandidatePost;
import com.findajobremotely.model.CandidateProfile;
import com.findajobremotely.model.Commission;
import com.findajobremotely.model.EmployerProfile;
import com.findajobremotely.model.Job;
import com.findajobremotely.model.User;
import com.findajobremotely.repository.ApplicationRepository;
import com.findajobremotely.repository.CandidateProfileRepository;
import com.findajobremotely.repository.EmployerProfileRepository;
import com.findajobremotely.repository.UserRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * The single place where entities become API responses. This is also where the privacy boundary
 * is enforced: public views never contain email, phone, salary expectation or resume details.
 */
@Component
public class ViewMapper {

    private final FieldCrypto crypto;
    private final UserRepository users;
    private final CandidateProfileRepository candidates;
    private final EmployerProfileRepository employers;
    private final ApplicationRepository applications;

    public ViewMapper(FieldCrypto crypto, UserRepository users, CandidateProfileRepository candidates,
                      EmployerProfileRepository employers, ApplicationRepository applications) {
        this.crypto = crypto;
        this.users = users;
        this.candidates = candidates;
        this.employers = employers;
        this.applications = applications;
    }

    // ---------------- helpers ----------------

    public Map<String, User> usersById(Collection<String> ids) {
        return users.findAllById(ids).stream().collect(Collectors.toMap(u -> u.id, Function.identity()));
    }

    public Map<String, CandidateProfile> candidateProfilesById(Collection<String> ids) {
        return candidates.findAllById(ids).stream().collect(Collectors.toMap(p -> p.userId, Function.identity()));
    }

    public String phone(CandidateProfile p) {
        return crypto.decrypt(p.phoneEnc, p.userId);
    }

    public Long expectedSalary(CandidateProfile p) {
        String s = crypto.decrypt(p.expectedMonthlySalaryEnc, p.userId);
        return s == null ? null : Long.valueOf(s);
    }

    public boolean candidateComplete(CandidateProfile p) {
        return p.headline != null && !p.headline.isBlank() && p.skills != null && !p.skills.isEmpty();
    }

    // ---------------- users ----------------

    public UserView user(User u) {
        return new UserView(u.id, u.email, u.fullName, u.role, u.status, u.createdAt, u.lastLoginAt);
    }

    // ---------------- candidates ----------------

    public CandidateProfileView candidateOwner(User u, CandidateProfile p) {
        return new CandidateProfileView(
                u.id, u.fullName, u.email,
                p.headline, p.about, p.skills, p.experienceLevel, p.yearsOfExperience,
                p.preferredJobTypes, p.preferredRoles, p.education, p.country, p.timezone,
                p.githubUrl, p.linkedinUrl, p.portfolioUrl,
                p.publicProfile, phone(p), expectedSalary(p), p.currency,
                p.resumeFileName != null, p.resumeFileName, p.resumeSizeBytes, p.resumeUploadedAt,
                candidateComplete(p));
    }

    public CandidatePublicView candidatePublic(User u, CandidateProfile p) {
        return new CandidatePublicView(
                p.userId, u == null ? "Unknown" : u.fullName, p.headline, p.about, p.skills,
                p.experienceLevel, p.yearsOfExperience, p.preferredJobTypes, p.preferredRoles,
                p.education, p.country, p.timezone, p.githubUrl, p.linkedinUrl, p.portfolioUrl);
    }

    public List<CandidatePublicView> candidatesPublic(List<CandidateProfile> profiles) {
        Map<String, User> byId = usersById(profiles.stream().map(p -> p.userId).collect(Collectors.toSet()));
        return profiles.stream().map(p -> candidatePublic(byId.get(p.userId), p)).toList();
    }

    /** Contact details are included - only call this for an employer who received the application. */
    public ApplicantView applicant(User u, CandidateProfile p) {
        return new ApplicantView(candidatePublic(u, p), u == null ? null : u.email, phone(p),
                p.resumeFileName != null, p.resumeFileName);
    }

    // ---------------- employers ----------------

    public boolean employerComplete(EmployerProfile p) {
        return p.companyName != null && !p.companyName.isBlank() && p.about != null && !p.about.isBlank();
    }

    public EmployerProfileView employerOwner(User u, EmployerProfile p) {
        return new EmployerProfileView(
                u.id, u.email, p.companyName, p.website, p.industry, p.companySize, p.about, p.country,
                p.foundedYear, p.verified, crypto.decrypt(p.contactPhoneEnc, p.userId), employerComplete(p));
    }

    public EmployerPublicView employerPublic(EmployerProfile p) {
        return new EmployerPublicView(p.userId, p.companyName, p.website, p.industry, p.companySize, p.about,
                p.country, p.foundedYear, p.verified);
    }

    // ---------------- jobs ----------------

    public JobView job(Job j, boolean companyVerified, Long applicantCount) {
        return new JobView(j.id, j.employerId, j.companyName, companyVerified, j.title, j.description, j.type,
                j.experienceLevel, j.skills, j.salaryMin, j.salaryMax, j.currency, j.remoteRegion, j.openings,
                j.hiredCount, j.status, j.createdAt, j.updatedAt, applicantCount);
    }

    public List<JobView> jobs(List<Job> list, boolean withApplicantCounts) {
        Set<String> employerIds = list.stream().map(j -> j.employerId).collect(Collectors.toSet());
        Map<String, Boolean> verified = new HashMap<>();
        employers.findAllById(employerIds).forEach(e -> verified.put(e.userId, e.verified));
        return list.stream()
                .map(j -> job(j, verified.getOrDefault(j.employerId, false),
                        withApplicantCounts ? Long.valueOf(applications.countByJobId(j.id)) : null))
                .toList();
    }

    // ---------------- posts ----------------

    public List<PostView> posts(List<CandidatePost> list) {
        Set<String> authorIds = list.stream().map(p -> p.authorId).collect(Collectors.toSet());
        Map<String, User> authors = usersById(authorIds);
        Map<String, CandidateProfile> profiles = candidateProfilesById(authorIds);
        return list.stream().map(p -> {
            User a = authors.get(p.authorId);
            CandidateProfile prof = profiles.get(p.authorId);
            return new PostView(p.id, p.authorId, a == null ? "Unknown" : a.fullName,
                    prof == null ? null : prof.headline, p.type, p.title, p.content, p.tags, p.status,
                    p.createdAt, p.updatedAt);
        }).toList();
    }

    // ---------------- commissions ----------------

    public List<CommissionView> commissions(List<Commission> list) {
        Map<String, User> byId = usersById(list.stream().map(c -> c.candidateId).collect(Collectors.toSet()));
        return list.stream().map(c -> {
            User cand = byId.get(c.candidateId);
            return new CommissionView(c.id, c.applicationId, c.jobId, c.jobTitle, c.companyName, c.candidateId,
                    cand == null ? "Deleted user" : cand.fullName, c.agreedMonthlySalary, c.currency, c.percent,
                    c.amount, c.status, c.notes, c.createdAt, c.paidAt);
        }).toList();
    }
}
