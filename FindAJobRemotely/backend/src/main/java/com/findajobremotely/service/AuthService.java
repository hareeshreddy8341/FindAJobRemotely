package com.findajobremotely.service;

import com.findajobremotely.config.AppProperties;
import com.findajobremotely.core.PasswordPolicy;
import com.findajobremotely.core.TextClean;
import com.findajobremotely.dto.AuthDtos.AuthResponse;
import com.findajobremotely.dto.AuthDtos.ChangePasswordRequest;
import com.findajobremotely.dto.AuthDtos.LoginRequest;
import com.findajobremotely.dto.AuthDtos.RegisterRequest;
import com.findajobremotely.dto.AuthDtos.UserView;
import com.findajobremotely.exception.ApiException;
import com.findajobremotely.model.AccountStatus;
import com.findajobremotely.model.CandidateProfile;
import com.findajobremotely.model.EmployerProfile;
import com.findajobremotely.model.RefreshToken;
import com.findajobremotely.model.Role;
import com.findajobremotely.model.User;
import com.findajobremotely.repository.CandidateProfileRepository;
import com.findajobremotely.repository.EmployerProfileRepository;
import com.findajobremotely.repository.RefreshTokenRepository;
import com.findajobremotely.repository.UserRepository;
import com.findajobremotely.security.AuthPrincipal;
import com.findajobremotely.security.JwtService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    /** The controller puts refreshToken into an HttpOnly cookie and sends only `response` in the body. */
    public record AuthResult(AuthResponse response, String refreshToken) {
    }

    /** Two tabs refreshing at the same instant is normal; a token replayed later is treated as theft. */
    private static final Duration REUSE_GRACE = Duration.ofSeconds(10);

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final CandidateProfileRepository candidateProfiles;
    private final EmployerProfileRepository employerProfiles;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AppProperties props;
    private final AuditService audit;
    private final AccountService accounts;
    private final ViewMapper mapper;
    private final MongoTemplate template;
    private final String dummyHash;

    public AuthService(UserRepository users, CandidateProfileRepository candidateProfiles,
                       EmployerProfileRepository employerProfiles, RefreshTokenRepository refreshTokens,
                       PasswordEncoder encoder, JwtService jwt, AppProperties props, AuditService audit,
                       AccountService accounts, ViewMapper mapper, MongoTemplate template) {
        this.users = users;
        this.candidateProfiles = candidateProfiles;
        this.employerProfiles = employerProfiles;
        this.refreshTokens = refreshTokens;
        this.encoder = encoder;
        this.jwt = jwt;
        this.props = props;
        this.audit = audit;
        this.accounts = accounts;
        this.mapper = mapper;
        this.template = template;
        // Compared against when the email is unknown, so "no such user" costs the same time as "wrong password"
        this.dummyHash = encoder.encode("not-a-real-password-used-for-timing-only");
    }

    // ------------------------------------------------------------------ register

    public AuthResult register(RegisterRequest req) {
        if (req.role() != Role.CANDIDATE && req.role() != Role.EMPLOYER) {
            throw ApiException.badRequest("Choose either a candidate or an employer account");
        }
        String email = normalizeEmail(req.email());
        String fullName = TextClean.line(req.fullName());
        if (fullName == null || fullName.length() < 2) {
            throw ApiException.badRequest("Please enter your full name");
        }
        String companyName = TextClean.line(req.companyName());
        if (req.role() == Role.EMPLOYER && (companyName == null || companyName.length() < 2)) {
            throw ApiException.badRequest("Company name is required for employer accounts");
        }
        String problem = PasswordPolicy.firstProblem(req.password(), email);
        if (problem != null) {
            throw ApiException.badRequest(problem);
        }
        if (users.existsByEmail(email)) {
            throw ApiException.conflict("An account with this email already exists");
        }

        User u = new User();
        u.email = email;
        u.fullName = fullName;
        u.role = req.role();
        u.passwordHash = encoder.encode(req.password());
        try {
            users.save(u);
        } catch (DuplicateKeyException e) {
            throw ApiException.conflict("An account with this email already exists");
        }

        if (u.role == Role.CANDIDATE) {
            CandidateProfile p = new CandidateProfile();
            p.userId = u.id;
            candidateProfiles.save(p);
        } else {
            EmployerProfile p = new EmployerProfile();
            p.userId = u.id;
            p.companyName = companyName;
            employerProfiles.save(p);
        }
        audit.log(u.id, u.email, "REGISTER", "USER", u.id, "role=" + u.role);
        return issueSession(u, UUID.randomUUID().toString());
    }

    // ------------------------------------------------------------------ login

    /**
     * @param adminPortal true for /api/auth/admin/login. Admin accounts can only sign in there, and
     *                    candidate / employer accounts can only sign in through the public form.
     */
    public AuthResult login(LoginRequest req, boolean adminPortal) {
        String email = normalizeEmail(req.email());
        Optional<User> found = users.findByEmail(email);
        if (found.isEmpty()) {
            encoder.matches(req.password(), dummyHash);
            throw invalidCredentials();
        }
        User u = found.get();
        Instant now = Instant.now();

        if (u.lockedUntil != null && u.lockedUntil.isAfter(now)) {
            encoder.matches(req.password(), dummyHash);
            throw invalidCredentials();
        }
        if (!encoder.matches(req.password(), u.passwordHash)) {
            registerFailure(u, now);
            throw invalidCredentials();
        }
        if ((u.role == Role.ADMIN) != adminPortal) {
            throw invalidCredentials();
        }
        if (u.status != AccountStatus.ACTIVE) {
            throw ApiException.forbidden("This account has been suspended. Please contact support.");
        }

        template.updateFirst(Query.query(Criteria.where("id").is(u.id)),
                new Update().set("lastLoginAt", now).set("failedLoginAttempts", 0).unset("lockedUntil"),
                User.class);
        u.lastLoginAt = now;
        if (u.role == Role.ADMIN) {
            audit.log(u.id, u.email, "ADMIN_LOGIN", "USER", u.id, null);
        }
        return issueSession(u, UUID.randomUUID().toString());
    }

    private void registerFailure(User u, Instant now) {
        int attempts = u.failedLoginAttempts + 1;
        Update update = new Update();
        if (attempts >= props.security().maxFailedLogins()) {
            update.set("failedLoginAttempts", 0)
                    .set("lockedUntil", now.plus(Duration.ofMinutes(props.security().lockMinutes())));
            audit.log(u.id, u.email, "ACCOUNT_LOCKED", "USER", u.id,
                    "too many failed logins; locked for " + props.security().lockMinutes() + " minutes");
        } else {
            update.set("failedLoginAttempts", attempts);
        }
        template.updateFirst(Query.query(Criteria.where("id").is(u.id)), update, User.class);
    }

    private static ApiException invalidCredentials() {
        return ApiException.unauthorized("Invalid email or password, or the account is temporarily locked");
    }

    // ------------------------------------------------------------------ refresh / logout

    public AuthResult refresh(String rawToken) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > 200) {
            throw ApiException.unauthorized("Session expired. Please sign in again.");
        }
        RefreshToken rt = refreshTokens.findByTokenHash(sha256Hex(rawToken))
                .orElseThrow(() -> ApiException.unauthorized("Session expired. Please sign in again."));
        Instant now = Instant.now();

        if (rt.expiresAt.isBefore(now)) {
            refreshTokens.deleteByFamilyId(rt.familyId);
            throw ApiException.unauthorized("Session expired. Please sign in again.");
        }
        if (rt.used) {
            boolean benignRace = rt.usedAt != null && Duration.between(rt.usedAt, now).compareTo(REUSE_GRACE) <= 0;
            if (!benignRace) {
                // An old token was replayed: assume it was stolen and end every session in this family.
                refreshTokens.deleteByFamilyId(rt.familyId);
                audit.log(rt.userId, null, "REFRESH_TOKEN_REUSE", "USER", rt.userId,
                        "A used refresh token was presented again; session family revoked");
                throw ApiException.unauthorized("Session expired. Please sign in again.");
            }
        } else {
            rt.used = true;
            rt.usedAt = now;
            refreshTokens.save(rt);
        }

        User u = users.findById(rt.userId)
                .filter(x -> x.status == AccountStatus.ACTIVE)
                .orElseThrow(() -> ApiException.unauthorized("Session expired. Please sign in again."));
        return issueSession(u, rt.familyId);
    }

    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > 200) {
            return;
        }
        refreshTokens.findByTokenHash(sha256Hex(rawToken))
                .ifPresent(rt -> refreshTokens.deleteByFamilyId(rt.familyId));
    }

    // ------------------------------------------------------------------ account

    public UserView me(AuthPrincipal me) {
        return mapper.user(load(me));
    }

    public AuthResult changePassword(AuthPrincipal me, ChangePasswordRequest req) {
        User u = load(me);
        if (!encoder.matches(req.currentPassword(), u.passwordHash)) {
            throw ApiException.badRequest("Your current password is incorrect");
        }
        if (req.currentPassword().equals(req.newPassword())) {
            throw ApiException.badRequest("Choose a password you have not used before");
        }
        String problem = PasswordPolicy.firstProblem(req.newPassword(), u.email);
        if (problem != null) {
            throw ApiException.badRequest(problem);
        }
        u.passwordHash = encoder.encode(req.newPassword());
        u.updatedAt = Instant.now();
        users.save(u);
        refreshTokens.deleteByUserId(u.id); // sign out every other device
        audit.log(me, "PASSWORD_CHANGED", "USER", u.id, null);
        return issueSession(u, UUID.randomUUID().toString());
    }

    public void deleteAccount(AuthPrincipal me, String password) {
        User u = load(me);
        if (u.role == Role.ADMIN) {
            throw ApiException.forbidden("Administrator accounts cannot be deleted here");
        }
        if (!encoder.matches(password, u.passwordHash)) {
            throw ApiException.badRequest("Your password is incorrect");
        }
        accounts.deleteUserAndData(u);
        audit.log(u.id, u.email, "ACCOUNT_DELETED", "USER", u.id, "self-service erasure");
    }

    // ------------------------------------------------------------------ internals

    private User load(AuthPrincipal me) {
        return users.findById(me.id()).orElseThrow(() -> ApiException.unauthorized("Account not found"));
    }

    private AuthResult issueSession(User u, String familyId) {
        String access = jwt.createAccessToken(u.id, u.role);
        String raw = randomToken();

        RefreshToken rt = new RefreshToken();
        rt.tokenHash = sha256Hex(raw);
        rt.userId = u.id;
        rt.familyId = familyId;
        rt.expiresAt = Instant.now().plus(Duration.ofDays(props.jwt().refreshTtlDays()));
        refreshTokens.save(rt);

        return new AuthResult(new AuthResponse(access, jwt.accessTtlSeconds(), mapper.user(u)), raw);
    }

    static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
