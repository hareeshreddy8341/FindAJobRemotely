package com.findajobremotely.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.findajobremotely.core.MatchingEngine.CandidateSignals;
import com.findajobremotely.core.MatchingEngine.JobSignals;
import com.findajobremotely.core.MatchingEngine.MatchResult;
import com.findajobremotely.model.ApplicationStatus;
import com.findajobremotely.model.ExperienceLevel;
import com.findajobremotely.model.JobType;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CoreLogicTest {

    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

    // ---------- Skills ----------

    @Test
    void skillAliasesResolveToOneCanonicalForm() {
        assertEquals("react", Skills.normalize("  ReactJS "));
        assertEquals("node.js", Skills.normalize("NodeJS"));
        assertEquals("javascript", Skills.normalize("JS"));
        assertEquals("c#", Skills.normalize("C Sharp"));
        assertEquals("rust", Skills.normalize("Rust"));
        assertEquals("", Skills.normalize(null));
    }

    @Test
    void skillCleanDeduplicatesByCanonicalFormAndCapsSize() {
        List<String> cleaned = Skills.clean(List.of("React", "reactjs", " REACT.JS ", "Java", "", "  "), 10);
        assertEquals(List.of("React", "Java"), cleaned);
        assertEquals(2, Skills.clean(List.of("a1", "b2", "c3", "d4"), 2).size());
        assertTrue(Skills.clean(null, 5).isEmpty());
    }

    // ---------- TextClean ----------

    @Test
    void lineStripsControlCharactersAndCollapsesWhitespace() {
        assertEquals("hello world", TextClean.line("  hello\n\t world \u0000 "));
        assertNull(TextClean.line(null));
    }

    @Test
    void blockKeepsNewlinesButDropsOtherControlCharacters() {
        assertEquals("a\nb\nc", TextClean.block("a\r\nb\rc\u0007"));
    }

    @Test
    void escapeRegexNeutralisesMetacharacters() {
        assertEquals("a\\.b\\*\\(c\\)\\[d\\]\\{e\\}\\|f\\^g\\$h\\+i\\?j\\\\k", TextClean.escapeRegex("a.b*(c)[d]{e}|f^g$h+i?j\\k"));
        assertTrue("c++".matches(TextClean.escapeRegex("c++")));
    }

    @Test
    void safeFileNameRemovesPathsAndOddCharacters() {
        assertEquals("cv.pdf", TextClean.safeFileName("../../etc/cv.pdf"));
        assertEquals("my_cv_.pdf", TextClean.safeFileName("C:\\users\\x\\my<cv>.pdf"));
        assertEquals("resume.pdf", TextClean.safeFileName(""));
        assertEquals("notes.pdf", TextClean.safeFileName("notes"));
    }

    @Test
    void pdfMagicNumberIsChecked() {
        assertTrue(TextClean.isPdf("%PDF-1.7 rest".getBytes(StandardCharsets.US_ASCII)));
        assertFalse(TextClean.isPdf("<html>".getBytes(StandardCharsets.US_ASCII)));
        assertFalse(TextClean.isPdf(new byte[2]));
        assertFalse(TextClean.isPdf(null));
    }

    // ---------- PasswordPolicy ----------

    @Test
    void passwordPolicyAcceptsAGoodPassword() {
        assertTrue(PasswordPolicy.problems("Sunrise-2026-Walk", "santhosh@example.com").isEmpty());
    }

    @Test
    void passwordPolicyRejectsWeakPasswords() {
        assertFalse(PasswordPolicy.problems("short1", "a@b.com").isEmpty());
        assertFalse(PasswordPolicy.problems("onlylettersonly", "a@b.com").isEmpty());
        assertFalse(PasswordPolicy.problems("12345678901234", "a@b.com").isEmpty());
        assertFalse(PasswordPolicy.problems("Password123", "a@b.com").isEmpty());
        assertFalse(PasswordPolicy.problems("santhosh12345", "santhosh@example.com").isEmpty());
        assertFalse(PasswordPolicy.problems("x".repeat(65) + "1", "a@b.com").isEmpty());
        assertNull(PasswordPolicy.firstProblem("Sunrise-2026-Walk", "a@b.com"));
    }

    // ---------- FieldCrypto ----------

    @Test
    void cryptoRoundTripsAndNeverRepeatsCiphertext() {
        FieldCrypto crypto = new FieldCrypto(KEY);
        String a = crypto.encrypt("+91 98765 43210", "user-1");
        String b = crypto.encrypt("+91 98765 43210", "user-1");
        assertNotEquals(a, b);
        assertTrue(a.startsWith("v1:"));
        assertFalse(a.contains("98765"));
        assertEquals("+91 98765 43210", crypto.decrypt(a, "user-1"));
        assertNull(crypto.encrypt(null, "user-1"));
        assertNull(crypto.decrypt(null, "user-1"));
    }

    @Test
    void cryptoBindsCiphertextToItsOwner() {
        FieldCrypto crypto = new FieldCrypto(KEY);
        String enc = crypto.encrypt("secret", "user-1");
        assertThrows(IllegalStateException.class, () -> crypto.decrypt(enc, "user-2"));
    }

    @Test
    void cryptoDetectsTampering() {
        FieldCrypto crypto = new FieldCrypto(KEY);
        byte[] enc = crypto.encryptBytes("%PDF-1.4 data".getBytes(StandardCharsets.US_ASCII), "u1");
        assertEquals("%PDF-1.4 data", new String(crypto.decryptBytes(enc, "u1"), StandardCharsets.US_ASCII));
        enc[enc.length - 1] ^= 0x01;
        assertThrows(IllegalStateException.class, () -> crypto.decryptBytes(enc, "u1"));
    }

    @Test
    void cryptoRejectsBadKeys() {
        assertThrows(IllegalArgumentException.class, () -> new FieldCrypto(""));
        assertThrows(IllegalArgumentException.class, () -> new FieldCrypto("not base64 !!"));
        assertThrows(IllegalArgumentException.class,
                () -> new FieldCrypto(Base64.getEncoder().encodeToString(new byte[16])));
    }

    // ---------- MatchingEngine ----------

    private static CandidateSignals candidate(Set<String> skills, Set<JobType> types, ExperienceLevel level,
                                              List<String> roles, Long salary, String currency) {
        return new CandidateSignals(skills, types, level, roles, salary, currency);
    }

    @Test
    void perfectMatchScoresOneHundred() {
        CandidateSignals c = candidate(Set.of("react", "typescript"), Set.of(JobType.FULL_TIME),
                ExperienceLevel.MID, List.of("Frontend Developer"), 3000L, "USD");
        JobSignals j = new JobSignals(Set.of("react", "typescript"), JobType.FULL_TIME, ExperienceLevel.MID,
                "Senior Frontend Developer", 4000L, "USD");
        MatchResult r = MatchingEngine.score(c, j);
        assertEquals(100, r.score());
        assertTrue(r.missingSkills().isEmpty());
        assertEquals(2, r.matchedSkills().size());
    }

    @Test
    void completeMismatchScoresZero() {
        CandidateSignals c = candidate(Set.of("java"), Set.of(JobType.PART_TIME), ExperienceLevel.SENIOR,
                List.of("Backend Engineer"), 9000L, "USD");
        JobSignals j = new JobSignals(Set.of("figma", "sketch"), JobType.INTERNSHIP, ExperienceLevel.FRESHER,
                "Design Intern", 100L, "USD");
        MatchResult r = MatchingEngine.score(c, j);
        assertEquals(0, r.score());
        assertEquals(Set.of("figma", "sketch"), Set.copyOf(r.missingSkills()));
    }

    @Test
    void sparseProfileGetsNeutralScoreNotZero() {
        CandidateSignals c = candidate(Set.of(), Set.of(), null, List.of(), null, null);
        JobSignals j = new JobSignals(Set.of("react"), JobType.FULL_TIME, ExperienceLevel.MID, "Dev", null, null);
        int score = MatchingEngine.score(c, j).score();
        // skills 0, everything else half marks: 7.5 + 7.5 + 5 + 2.5 = 22.5
        assertEquals(23, score);
    }

    @Test
    void freshersFitInternshipsRegardlessOfLevelLabel() {
        assertEquals(1.0, MatchingEngine.experienceScore(ExperienceLevel.FRESHER, ExperienceLevel.MID, JobType.INTERNSHIP));
        assertEquals(0.5, MatchingEngine.experienceScore(ExperienceLevel.JUNIOR, ExperienceLevel.MID, JobType.FULL_TIME));
        assertEquals(0.0, MatchingEngine.experienceScore(ExperienceLevel.FRESHER, ExperienceLevel.SENIOR, JobType.FULL_TIME));
    }

    @Test
    void salaryComparisonIgnoresDifferentCurrencies() {
        assertEquals(1.0, MatchingEngine.salaryScore(2000L, "USD", 3000L, "USD"));
        assertEquals(0.5, MatchingEngine.salaryScore(3200L, "USD", 3000L, "USD"));
        assertEquals(0.0, MatchingEngine.salaryScore(5000L, "USD", 3000L, "USD"));
        assertEquals(0.5, MatchingEngine.salaryScore(5000L, "INR", 3000L, "USD"));
    }

    @Test
    void roleScoreMatchesTokensInTitle() {
        assertEquals(1.0, MatchingEngine.roleScore(List.of("data scientist"), "Remote Data Scientist (NLP)"));
        assertEquals(0.5, MatchingEngine.roleScore(List.of("data scientist"), "Data Engineer"));
        assertEquals(0.0, MatchingEngine.roleScore(List.of("data scientist"), "Sales Lead"));
    }

    // ---------- ApplicationFlow ----------

    @Test
    void employerTransitionsFollowTheRules() {
        assertTrue(ApplicationFlow.employerMayMove(ApplicationStatus.APPLIED, ApplicationStatus.SHORTLISTED));
        assertTrue(ApplicationFlow.employerMayMove(ApplicationStatus.INTERVIEW, ApplicationStatus.HIRED));
        assertTrue(ApplicationFlow.employerMayMove(ApplicationStatus.OFFERED, ApplicationStatus.REJECTED));
        assertFalse(ApplicationFlow.employerMayMove(ApplicationStatus.HIRED, ApplicationStatus.REJECTED));
        assertFalse(ApplicationFlow.employerMayMove(ApplicationStatus.REJECTED, ApplicationStatus.INTERVIEW));
        assertFalse(ApplicationFlow.employerMayMove(ApplicationStatus.WITHDRAWN, ApplicationStatus.HIRED));
        assertFalse(ApplicationFlow.employerMayMove(ApplicationStatus.APPLIED, ApplicationStatus.APPLIED));
        assertFalse(ApplicationFlow.employerMayMove(ApplicationStatus.SHORTLISTED, ApplicationStatus.WITHDRAWN));
        assertFalse(ApplicationFlow.employerMayMove(ApplicationStatus.SHORTLISTED, ApplicationStatus.APPLIED));
    }

    @Test
    void candidatesCanOnlyWithdrawOpenApplications() {
        assertTrue(ApplicationFlow.candidateMayWithdraw(ApplicationStatus.APPLIED));
        assertTrue(ApplicationFlow.candidateMayWithdraw(ApplicationStatus.OFFERED));
        assertFalse(ApplicationFlow.candidateMayWithdraw(ApplicationStatus.HIRED));
        assertFalse(ApplicationFlow.candidateMayWithdraw(ApplicationStatus.REJECTED));
    }

    // ---------- CommissionCalculator ----------

    @Test
    void commissionIsAPercentageRoundedHalfUp() {
        assertEquals(300L, CommissionCalculator.amount(3000, new BigDecimal("10")));
        assertEquals(123L, CommissionCalculator.amount(1234, new BigDecimal("10")));
        assertEquals(63L, CommissionCalculator.amount(1250, new BigDecimal("5")));
        assertEquals(0L, CommissionCalculator.amount(1000, BigDecimal.ZERO));
    }

    @Test
    void commissionRejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> CommissionCalculator.amount(0, BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class, () -> CommissionCalculator.amount(100, new BigDecimal("101")));
        assertThrows(IllegalArgumentException.class, () -> CommissionCalculator.amount(100, new BigDecimal("-1")));
    }
}
