package com.hackathon.leave.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Typed view of the {@code leave.*} block in application.yml. */
@ConfigurationProperties(prefix = "leave")
public record LeaveProperties(int annualQuotaDefault, int escalationTimeoutMinutes, int escalationCheckSeconds,
                              double conflictThreshold, Jwt jwt, String demoNow) {

    public record Jwt(String secret, int expiryMinutes) {}
}
