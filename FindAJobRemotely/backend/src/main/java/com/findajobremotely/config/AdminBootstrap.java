package com.findajobremotely.config;

import com.findajobremotely.core.PasswordPolicy;
import com.findajobremotely.model.Role;
import com.findajobremotely.model.User;
import com.findajobremotely.repository.UserRepository;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first administrator from environment variables. There is deliberately no public
 * endpoint that can create an admin.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AppProperties props;

    public AdminBootstrap(UserRepository users, PasswordEncoder encoder, AppProperties props) {
        this.users = users;
        this.encoder = encoder;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.existsByRole(Role.ADMIN)) {
            return;
        }
        String email = props.admin().email();
        String password = props.admin().password();
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.warn("No administrator exists. Set ADMIN_EMAIL and ADMIN_PASSWORD and restart to create one.");
            return;
        }
        String problem = PasswordPolicy.firstProblem(password, email);
        if (problem != null) {
            throw new IllegalStateException("ADMIN_PASSWORD is not acceptable: " + problem);
        }
        User admin = new User();
        admin.email = email.trim().toLowerCase(Locale.ROOT);
        admin.fullName = props.admin().name() == null ? "Platform Admin" : props.admin().name();
        admin.role = Role.ADMIN;
        admin.passwordHash = encoder.encode(password);
        users.save(admin);
        log.info("Created administrator account {}", admin.email);
    }
}
