package com.example.pos_system.model.service.auth;

import com.example.pos_system.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private record Attempt(int count, Instant firstFailure) {
        boolean expired(Instant now) {
            return firstFailure.plus(WINDOW).isBefore(now);
        }
    }

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    /** Called before password verification */
    public void checkAllowed(String key) {
        Attempt a = attempts.get(key);
        if (a == null) return;
        if (a.expired(Instant.now())) {
            attempts.remove(key, a);
            return;
        }
        if (a.count() >= MAX_ATTEMPTS) {
            throw new BusinessException(
                    "Too many failed login attempts. Please wait 15 minutes and try again.", HttpStatus.TOO_MANY_REQUESTS
            );
        }
    }

    public void recordFailure(String key) {
        Instant now = Instant.now();
        attempts.merge(key, new Attempt(1, now),
                (old, fresh) -> old.expired(now) ? fresh : new Attempt(old.count() + 1, old.firstFailure()));
        if (attempts.size() > 10_000) {   // ការពារ Memory ពេញ
            attempts.values().removeIf(a -> a.expired(now));
        }
    }

    public void recordSuccess(String key) {
        attempts.remove(key);
    }

}
