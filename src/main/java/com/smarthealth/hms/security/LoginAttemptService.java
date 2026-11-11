package com.smarthealth.hms.security;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple brute-force protection: after {@link #MAX_FAILURES} consecutive failed logins a username is
 * locked for {@link #LOCK_DURATION}. State is kept in memory (resets when the application restarts).
 */
@Service
public class LoginAttemptService {

    public static final int MAX_FAILURES = 5;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private static final class Attempt {
        int failures;
        Instant lockedUntil;
    }

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginAttemptService(Clock clock) {
        this.clock = clock;
    }

    public void loginFailed(String username) {
        if (username == null) {
            return;
        }
        attempts.compute(key(username), (k, attempt) -> {
            Instant now = clock.instant();
            if (attempt == null || (attempt.lockedUntil != null && now.isAfter(attempt.lockedUntil))) {
                attempt = new Attempt();
            }
            attempt.failures++;
            if (attempt.failures >= MAX_FAILURES) {
                attempt.lockedUntil = now.plus(LOCK_DURATION);
            }
            return attempt;
        });
    }

    public void loginSucceeded(String username) {
        if (username != null) {
            attempts.remove(key(username));
        }
    }

    public boolean isBlocked(String username) {
        if (username == null) {
            return false;
        }
        Attempt attempt = attempts.get(key(username));
        return attempt != null && attempt.lockedUntil != null && clock.instant().isBefore(attempt.lockedUntil);
    }

    private String key(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
