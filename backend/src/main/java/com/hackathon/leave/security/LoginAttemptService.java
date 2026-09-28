package com.hackathon.leave.security;

import com.hackathon.leave.config.LeaveProperties;
import com.hackathon.leave.exception.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Brute-force protection: after N consecutive failures for an email the account is locked for a while. Applies to
 * unknown emails too, so the lock does not reveal which accounts exist. In-memory (per instance).
 */
@Service
public class LoginAttemptService {

    private record State(int failures, Instant lockedUntil) {}

    private final ConcurrentHashMap<String, State> attempts = new ConcurrentHashMap<>();
    private final LeaveProperties.Login cfg;
    private final Clock clock;

    public LoginAttemptService(LeaveProperties props, Clock clock) {
        this.cfg = props.login();
        this.clock = clock;
    }

    /** @throws ApiException 429 while the account is locked */
    public void assertNotLocked(String email) {
        State s = attempts.get(key(email));
        if (s != null && s.lockedUntil() != null) {
            Instant now = clock.instant();
            if (now.isBefore(s.lockedUntil())) {
                long minutes = Math.max(1, Duration.between(now, s.lockedUntil()).plusSeconds(59).toMinutes());
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_ATTEMPTS",
                        "Too many failed sign-in attempts. Try again in " + minutes + " minute(s).");
            }
            attempts.remove(key(email));
        }
    }

    public void recordFailure(String email) {
        attempts.compute(key(email), (k, old) -> {
            int failures = (old == null ? 0 : old.failures()) + 1;
            return failures >= cfg.maxAttempts()
                    ? new State(failures, clock.instant().plus(Duration.ofMinutes(cfg.lockMinutes())))
                    : new State(failures, null);
        });
    }

    public void recordSuccess(String email) {
        attempts.remove(key(email));
    }

    private static String key(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
