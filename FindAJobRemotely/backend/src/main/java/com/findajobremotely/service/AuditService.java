package com.findajobremotely.service;

import com.findajobremotely.config.AppProperties;
import com.findajobremotely.core.TextClean;
import com.findajobremotely.model.AuditLog;
import com.findajobremotely.repository.AuditLogRepository;
import com.findajobremotely.security.AuthPrincipal;
import com.findajobremotely.security.ClientIp;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Writes the audit trail. Never throws: a logging problem must not break the action being logged. */
@Service
public class AuditService {

    private final AuditLogRepository repo;
    private final AppProperties props;

    public AuditService(AuditLogRepository repo, AppProperties props) {
        this.repo = repo;
        this.props = props;
    }

    public void log(AuthPrincipal actor, String action, String targetType, String targetId, String details) {
        log(actor == null ? null : actor.id(), actor == null ? null : actor.email(), action, targetType, targetId,
                details);
    }

    public void log(String actorId, String actorEmail, String action, String targetType, String targetId,
                    String details) {
        try {
            AuditLog a = new AuditLog();
            a.actorId = actorId;
            a.actorEmail = actorEmail;
            a.action = action;
            a.targetType = targetType;
            a.targetId = targetId;
            String d = TextClean.line(details);
            a.details = d != null && d.length() > 500 ? d.substring(0, 500) : d;
            a.ip = currentIp();
            repo.save(a);
        } catch (RuntimeException ignored) {
            // intentionally swallowed
        }
    }

    private String currentIp() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            return ClientIp.resolve(sra.getRequest(), props.security().trustForwardedHeaders());
        }
        return null;
    }
}
