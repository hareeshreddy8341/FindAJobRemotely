package com.findajobremotely.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Fixed-window, per-IP request limiter. Login and registration get a much smaller budget than the
 * rest of the API. Counters live in memory (bounded and self-expiring); run a shared store such as
 * Redis if you scale to several backend instances.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> SENSITIVE = Set.of(
            "/api/auth/login", "/api/auth/admin/login", "/api/auth/register");

    private final Cache<String, AtomicInteger> authBuckets = newCache();
    private final Cache<String, AtomicInteger> apiBuckets = newCache();
    private final int authLimit;
    private final int apiLimit;
    private final boolean trustForwarded;

    public RateLimitFilter(int authLimitPerMinute, int apiLimitPerMinute, boolean trustForwarded) {
        this.authLimit = authLimitPerMinute;
        this.apiLimit = apiLimitPerMinute;
        this.trustForwarded = trustForwarded;
    }

    private static Cache<String, AtomicInteger> newCache() {
        return Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(1)).maximumSize(100_000).build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/") || "OPTIONS".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String ip = ClientIp.resolve(request, trustForwarded);
        boolean sensitive = SENSITIVE.contains(request.getRequestURI()) && "POST".equals(request.getMethod());

        Cache<String, AtomicInteger> cache = sensitive ? authBuckets : apiBuckets;
        int limit = sensitive ? authLimit : apiLimit;
        int count = cache.get(ip, k -> new AtomicInteger()).incrementAndGet();

        if (count > limit) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Too many requests. Please wait a minute and try again.\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
