package com.findajobremotely.security;

import com.findajobremotely.model.AccountStatus;
import com.findajobremotely.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads "Authorization: Bearer ...", verifies the JWT and then re-loads the user from MongoDB.
 * The role comes from the database rather than the token, and suspended or deleted accounts are
 * rejected immediately even if their access token has not expired yet.
 *
 * <p>Deliberately NOT a Spring bean: it is created inside SecurityConfig so Boot does not also
 * register it as a plain servlet filter.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository users;

    public JwtAuthFilter(JwtService jwtService, UserRepository users) {
        this.jwtService = jwtService;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            jwtService.parse(token)
                    .flatMap(parsed -> users.findById(parsed.userId()))
                    .filter(user -> user.status == AccountStatus.ACTIVE)
                    .ifPresent(user -> {
                        AuthPrincipal principal = new AuthPrincipal(user.id, user.email, user.role);
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.role.name())));
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    });
        }
        chain.doFilter(request, response);
    }
}
