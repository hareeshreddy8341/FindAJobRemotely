package com.findajobremotely.security;

import com.findajobremotely.config.AppProperties;
import com.findajobremotely.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/** Issues and verifies short-lived HS256 access tokens. */
@Component
public class JwtService {

    public record ParsedToken(String userId, Role role) {
    }

    private final SecretKey key;
    private final String issuer;
    private final Duration accessTtl;

    public JwtService(AppProperties props) {
        String secret = props.jwt().secret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET is missing or too short (min 32 characters). Generate one with: openssl rand -base64 48");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = props.jwt().issuer();
        this.accessTtl = Duration.ofMinutes(props.jwt().accessTtlMinutes());
    }

    public long accessTtlSeconds() {
        return accessTtl.toSeconds();
    }

    public String createAccessToken(String userId, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(issuer)
                .subject(userId)
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /** Empty for anything that is not a valid, unexpired token signed with our key. */
    public Optional<ParsedToken> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String role = claims.get("role", String.class);
            return Optional.of(new ParsedToken(claims.getSubject(), Role.valueOf(role)));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }
}
