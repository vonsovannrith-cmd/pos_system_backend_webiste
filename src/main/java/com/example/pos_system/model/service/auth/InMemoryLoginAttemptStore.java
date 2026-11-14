package com.example.pos_system.model.service.auth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryLoginAttemptStore implements LoginAttemptStore {

    private record Attempt(int count, Instant firstFailure) {
        boolean expired(Instant now) {
            return firstFailure.plus(WINDOW).isBefore(now);
        }
    }

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    @Override
    public int failures(String key) {
        Attempt a = attempts.get(key);
        return (a == null || a.expired(Instant.now())) ? 0 : a.count();
    }

    @Override
    public void recordFailure(String key) {
        Instant now = Instant.now();
        attempts.merge(key, new Attempt(1, now),
                (old, fresh) -> old.expired(now) ? fresh : new Attempt(old.count() + 1, old.firstFailure()));
        if (attempts.size() > 10_000) {
            attempts.values().removeIf(a -> a.expired(now));
        }
    }

    @Override
    public void reset(String key) {
        attempts.remove(key);
    }
}
