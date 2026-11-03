package com.findajobremotely.core;

import com.findajobremotely.model.ExperienceLevel;
import com.findajobremotely.model.JobType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Transparent, explainable job-to-candidate scoring (0-100).
 *
 * <pre>
 *   skills overlap        55   share of the job's skills the candidate has
 *   job type preference   15   candidate prefers this kind of engagement (full-time, internship ...)
 *   experience fit        15   same level = full, one level apart = half
 *   role keywords         10   candidate's preferred roles appear in the job title
 *   pay expectation        5   candidate's expectation is within the job's maximum
 * </pre>
 *
 * Missing information scores half marks for that component instead of zero, so sparse profiles
 * are neither unfairly punished nor over-rewarded.
 */
public final class MatchingEngine {

    private MatchingEngine() {
    }

    public record CandidateSignals(Set<String> skills, Set<JobType> jobTypes, ExperienceLevel level,
                                   List<String> roles, Long expectedMonthlySalary, String currency) {
    }

    public record JobSignals(Set<String> skills, JobType type, ExperienceLevel level, String title,
                             Long salaryMax, String currency) {
    }

    public record MatchResult(int score, List<String> matchedSkills, List<String> missingSkills) {
    }

    public static MatchResult score(CandidateSignals c, JobSignals j) {
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String s : j.skills()) {
            if (c.skills().contains(s)) {
                matched.add(s);
            } else {
                missing.add(s);
            }
        }
        double skill = j.skills().isEmpty() ? 0.5 : (double) matched.size() / j.skills().size();
        double type = c.jobTypes().isEmpty() ? 0.5 : (c.jobTypes().contains(j.type()) ? 1.0 : 0.0);
        double exp = experienceScore(c.level(), j.level(), j.type());
        double role = roleScore(c.roles(), j.title());
        double pay = salaryScore(c.expectedMonthlySalary(), c.currency(), j.salaryMax(), j.currency());

        int total = (int) Math.round(55 * skill + 15 * type + 15 * exp + 10 * role + 5 * pay);
        return new MatchResult(Math.max(0, Math.min(100, total)), matched, missing);
    }

    static double experienceScore(ExperienceLevel candidate, ExperienceLevel job, JobType jobType) {
        if (jobType == JobType.INTERNSHIP && candidate == ExperienceLevel.FRESHER) {
            return 1.0;
        }
        if (candidate == null || job == null) {
            return 0.5;
        }
        int distance = Math.abs(candidate.ordinal() - job.ordinal());
        return distance == 0 ? 1.0 : distance == 1 ? 0.5 : 0.0;
    }

    static double roleScore(List<String> roles, String title) {
        if (roles == null || roles.isEmpty() || title == null) {
            return 0.5;
        }
        String t = title.toLowerCase(Locale.ROOT);
        double best = 0.0;
        for (String role : roles) {
            String[] tokens = Arrays.stream(role.toLowerCase(Locale.ROOT).split("[^a-z0-9+#.]+"))
                    .filter(x -> x.length() >= 3)
                    .toArray(String[]::new);
            if (tokens.length == 0) {
                continue;
            }
            long hits = Arrays.stream(tokens).filter(t::contains).count();
            best = Math.max(best, (double) hits / tokens.length);
        }
        return best;
    }

    static double salaryScore(Long expected, String expectedCurrency, Long jobMax, String jobCurrency) {
        if (expected == null || expected <= 0 || jobMax == null || jobMax <= 0) {
            return 0.5;
        }
        if (expectedCurrency != null && jobCurrency != null && !expectedCurrency.equalsIgnoreCase(jobCurrency)) {
            return 0.5;
        }
        if (expected <= jobMax) {
            return 1.0;
        }
        return jobMax >= expected * 0.8 ? 0.5 : 0.0;
    }
}
