package com.findajobremotely.controller;

import com.findajobremotely.config.AppProperties;
import com.findajobremotely.dto.AuthDtos.AuthResponse;
import com.findajobremotely.dto.AuthDtos.ChangePasswordRequest;
import com.findajobremotely.dto.AuthDtos.DeleteAccountRequest;
import com.findajobremotely.dto.AuthDtos.LoginRequest;
import com.findajobremotely.dto.AuthDtos.RegisterRequest;
import com.findajobremotely.dto.AuthDtos.UserView;
import com.findajobremotely.dto.CommonDtos.MessageResponse;
import com.findajobremotely.security.AuthPrincipal;
import com.findajobremotely.service.AuthService;
import com.findajobremotely.service.AuthService.AuthResult;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    static final String COOKIE = "fajr_refresh";

    private final AuthService auth;
    private final AppProperties props;

    public AuthController(AuthService auth, AppProperties props) {
        this.auth = auth;
        this.props = props;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return withCookie(HttpStatus.CREATED, auth.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return withCookie(HttpStatus.OK, auth.login(req, false));
    }

    @PostMapping("/admin/login")
    public ResponseEntity<AuthResponse> adminLogin(@Valid @RequestBody LoginRequest req) {
        return withCookie(HttpStatus.OK, auth.login(req, true));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue(name = COOKIE, required = false) String token) {
        return withCookie(HttpStatus.OK, auth.refresh(token));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@CookieValue(name = COOKIE, required = false) String token) {
        auth.logout(token);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
                .body(new MessageResponse("Signed out"));
    }

    @GetMapping("/me")
    public UserView me(@AuthenticationPrincipal AuthPrincipal me) {
        return auth.me(me);
    }

    @PostMapping("/change-password")
    public ResponseEntity<AuthResponse> changePassword(@AuthenticationPrincipal AuthPrincipal me,
                                                       @Valid @RequestBody ChangePasswordRequest req) {
        return withCookie(HttpStatus.OK, auth.changePassword(me, req));
    }

    @DeleteMapping("/account")
    public ResponseEntity<MessageResponse> deleteAccount(@AuthenticationPrincipal AuthPrincipal me,
                                                         @Valid @RequestBody DeleteAccountRequest req) {
        auth.deleteAccount(me, req.password());
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
                .body(new MessageResponse("Your account and data have been deleted"));
    }

    private ResponseEntity<AuthResponse> withCookie(HttpStatus status, AuthResult result) {
        Duration ttl = Duration.ofDays(props.jwt().refreshTtlDays());
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookie(result.refreshToken(), ttl).toString())
                .body(result.response());
    }

    /** HttpOnly keeps the token away from JavaScript; SameSite=Strict keeps it off cross-site requests. */
    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE, value)
                .httpOnly(true)
                .secure(props.cookie().secure())
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(maxAge)
                .build();
    }
}
