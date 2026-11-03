package com.findajobremotely.core;

import com.findajobremotely.model.ApplicationStatus;
import java.util.EnumSet;
import java.util.Set;

/** Allowed status transitions for a job application. */
public final class ApplicationFlow {

    private static final Set<ApplicationStatus> TERMINAL =
            EnumSet.of(ApplicationStatus.HIRED, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN);

    /** Statuses an employer may set. APPLIED and WITHDRAWN are never set by an employer. */
    private static final Set<ApplicationStatus> EMPLOYER_TARGETS = EnumSet.of(
            ApplicationStatus.SHORTLISTED, ApplicationStatus.INTERVIEW, ApplicationStatus.OFFERED,
            ApplicationStatus.HIRED, ApplicationStatus.REJECTED);

    private ApplicationFlow() {
    }

    public static boolean isTerminal(ApplicationStatus status) {
        return TERMINAL.contains(status);
    }

    public static boolean employerMayMove(ApplicationStatus from, ApplicationStatus to) {
        return !isTerminal(from) && from != to && EMPLOYER_TARGETS.contains(to);
    }

    public static boolean candidateMayWithdraw(ApplicationStatus from) {
        return !isTerminal(from);
    }
}
