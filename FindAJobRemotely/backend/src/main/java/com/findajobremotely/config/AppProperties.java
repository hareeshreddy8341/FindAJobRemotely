package com.findajobremotely.config;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cookie cookie, Cors cors, Security security, Admin admin, Commission commission) {

    public record Jwt(String secret, String issuer, long accessTtlMinutes, long refreshTtlDays) {
    }

    public record Cookie(boolean secure) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record Security(String dataEncryptionKey, boolean trustForwardedHeaders, int authRatePerMinute,
                           int apiRatePerMinute, int maxFailedLogins, long lockMinutes) {
    }

    public record Admin(String email, String password, String name) {
    }

    public record Commission(BigDecimal percent) {
    }
}
