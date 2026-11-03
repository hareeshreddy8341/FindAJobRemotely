package com.findajobremotely.service;

import com.findajobremotely.core.FieldCrypto;
import com.findajobremotely.core.Skills;
import com.findajobremotely.core.TextClean;
import com.findajobremotely.dto.ProfileDtos.CandidateProfileRequest;
import com.findajobremotely.dto.ProfileDtos.CandidateProfileView;
import com.findajobremotely.dto.ProfileDtos.EmployerProfileRequest;
import com.findajobremotely.dto.ProfileDtos.EmployerProfileView;
import com.findajobremotely.exception.ApiException;
import com.findajobremotely.model.CandidateProfile;
import com.findajobremotely.model.EmployerProfile;
import com.findajobremotely.model.JobType;
import com.findajobremotely.model.User;
import com.findajobremotely.repository.CandidateProfileRepository;
import com.findajobremotely.repository.EmployerProfileRepository;
import com.findajobremotely.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

    private final UserRepository users;
    private final CandidateProfileRepository candidates;
    private final EmployerProfileRepository employers;
    private final FieldCrypto crypto;
    private final ViewMapper mapper;

    public ProfileService(UserRepository users, CandidateProfileRepository candidates,
                          EmployerProfileRepository employers, FieldCrypto crypto, ViewMapper mapper) {
        this.users = users;
        this.candidates = candidates;
        this.employers = employers;
        this.crypto = crypto;
        this.mapper = mapper;
    }

    // ---------------- candidate ----------------

    public CandidateProfileView getCandidate(String userId) {
        User u = user(userId);
        return mapper.candidateOwner(u, candidateProfile(userId));
    }

    public CandidateProfileView updateCandidate(String userId, CandidateProfileRequest req) {
        User u = user(userId);
        CandidateProfile p = candidateProfile(userId);

        if (req.fullName() != null) {
            String name = TextClean.line(req.fullName());
            if (name == null || name.length() < 2) {
                throw ApiException.badRequest("Please enter your full name");
            }
            u.fullName = name;
            u.updatedAt = Instant.now();
            users.save(u);
        }

        p.headline = blankToNull(TextClean.line(req.headline()));
        p.about = blankToNull(TextClean.block(req.about()));
        p.skills = Skills.clean(req.skills(), 30);
        p.experienceLevel = req.experienceLevel();
        p.yearsOfExperience = req.yearsOfExperience();
        p.preferredJobTypes = distinct(req.preferredJobTypes());
        p.preferredRoles = Skills.clean(req.preferredRoles(), 10);
        p.education = blankToNull(TextClean.line(req.education()));
        p.country = blankToNull(TextClean.line(req.country()));
        p.timezone = blankToNull(TextClean.line(req.timezone()));
        p.githubUrl = blankToNull(trim(req.githubUrl()));
        p.linkedinUrl = blankToNull(trim(req.linkedinUrl()));
        p.portfolioUrl = blankToNull(trim(req.portfolioUrl()));
        if (req.publicProfile() != null) {
            p.publicProfile = req.publicProfile();
        }

        String phone = blankToNull(TextClean.line(req.phone()));
        p.phoneEnc = phone == null ? null : crypto.encrypt(phone, userId);

        String currency = blankToNull(trim(req.currency()));
        if (req.expectedMonthlySalary() != null && currency == null) {
            throw ApiException.badRequest("Select a currency for your expected salary");
        }
        p.expectedMonthlySalaryEnc = req.expectedMonthlySalary() == null
                ? null : crypto.encrypt(String.valueOf(req.expectedMonthlySalary()), userId);
        p.currency = currency;

        p.updatedAt = Instant.now();
        candidates.save(p);
        return mapper.candidateOwner(u, p);
    }

    private CandidateProfile candidateProfile(String userId) {
        return candidates.findById(userId).orElseGet(() -> {
            CandidateProfile p = new CandidateProfile();
            p.userId = userId;
            return candidates.save(p);
        });
    }

    // ---------------- employer ----------------

    public EmployerProfileView getEmployer(String userId) {
        return mapper.employerOwner(user(userId), employerProfile(userId));
    }

    public EmployerProfileView updateEmployer(String userId, EmployerProfileRequest req) {
        User u = user(userId);
        EmployerProfile p = employerProfile(userId);

        String company = TextClean.line(req.companyName());
        if (company == null || company.length() < 2) {
            throw ApiException.badRequest("Company name is required");
        }
        p.companyName = company;
        p.website = blankToNull(trim(req.website()));
        p.industry = blankToNull(TextClean.line(req.industry()));
        p.companySize = blankToNull(TextClean.line(req.companySize()));
        p.about = blankToNull(TextClean.block(req.about()));
        p.country = blankToNull(TextClean.line(req.country()));
        p.foundedYear = req.foundedYear();
        String phone = blankToNull(TextClean.line(req.contactPhone()));
        p.contactPhoneEnc = phone == null ? null : crypto.encrypt(phone, userId);
        p.updatedAt = Instant.now();
        employers.save(p);
        return mapper.employerOwner(u, p);
    }

    private EmployerProfile employerProfile(String userId) {
        return employers.findById(userId).orElseGet(() -> {
            EmployerProfile p = new EmployerProfile();
            p.userId = userId;
            p.companyName = user(userId).fullName;
            return employers.save(p);
        });
    }

    // ---------------- helpers ----------------

    private User user(String userId) {
        return users.findById(userId).orElseThrow(() -> ApiException.notFound("Account not found"));
    }

    private static List<JobType> distinct(List<JobType> in) {
        return in == null ? new ArrayList<>() : new ArrayList<>(new LinkedHashSet<>(in));
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
