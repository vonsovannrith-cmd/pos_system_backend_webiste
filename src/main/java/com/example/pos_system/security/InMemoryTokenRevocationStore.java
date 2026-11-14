package com.example.pos_system.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryTokenRevocationStore implements TokenRevocationStore{

    private final ConcurrentHashMap<String, Instant> revoked = new ConcurrentHashMap<>();

    @Override
    public void revoke(String jti, Instant expiresAt) {
        revoked.put(jti, expiresAt);
        if (revoked.size() > 10_000) {
            Instant now = Instant.now();
            revoked.values().removeIf(exp -> !exp.isAfter(now));
        }
    }

    @Override
    public boolean isRevoked(String jti) {
        Instant exp = revoked.get(jti);
        if (exp == null) return false;
        if (!exp.isAfter(Instant.now())) {      // Token has expired; no need to remember it.
            revoked.remove(jti);
            return false;
        }
        return true;
    }
}
