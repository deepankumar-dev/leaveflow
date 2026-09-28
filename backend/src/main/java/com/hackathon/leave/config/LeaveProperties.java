package com.hackathon.leave.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Typed view of the {@code leave.*} block in application.yml. */
@ConfigurationProperties(prefix = "leave")
public record LeaveProperties(int annualQuotaDefault, int escalationTimeoutMinutes, int escalationCheckSeconds,
                              double conflictThreshold, Jwt jwt, String demoNow, String corsOrigins, Login login,
                              Mail mail, Bootstrap bootstrap) {

    public record Jwt(String secret, int expiryMinutes) {}

    public record Login(int maxAttempts, int lockMinutes) {}

    public record Mail(String from) {}

    /** First-run administrator; a blank email means "do not bootstrap". */
    public record Bootstrap(String adminName, String adminEmail, String adminPassword) {}
}
