package com.example.pos_system.security;

import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.Token;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true")
public class RedisTokenRevocationStore implements TokenRevocationStore {

    private static final String PREFIX = "revoked:";

    private final StringRedisTemplate redis;
    private final Duration recheckAfter;

    /** jti known to have Logged Out (this instance or found in Redis) */
    private final Map<String, Instant> revokedLocal = new ConcurrentHashMap<>();

    /** JTIs already queried from Redis that are "not yet logged out" as of this moment */
    private final Map<String, Instant> okUntil = new ConcurrentHashMap<>();

    public RedisTokenRevocationStore(StringRedisTemplate redis,
                                     @Value("${app.redis.revocation-cache-seconds:60}") long cacheSeconds) {
        this.redis = redis;
        this.recheckAfter = Duration.ofSeconds(cacheSeconds);
    }

    @Override
    public void revoke(String jti, Instant expiresAt) {
        revokedLocal.put(jti, expiresAt);       // Takes effect immediately on this instance
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) return;   // Expired and no longer needed.
        try {
            redis.opsForValue().set(PREFIX + jti, "1", ttl);   // Delete itself when the token expires
        } catch (RuntimeException e) {
            log.warn("Redis unavailable: cannot store revoked token ({})", e.getMessage());
        }
    }

    @Override
    public boolean isRevoked(String jti) {
        Instant now = Instant.now();

        Instant exp = revokedLocal.get(jti);
        if (exp != null) {
            if (exp.isAfter(now)) return true;
            revokedLocal.remove(jti);
        }

        Instant ok = okUntil.get(jti);
        if (ok != null && ok.isAfter(now)) return false;   // Just asked; won't ask again.

        try {
            boolean revoked = Boolean.TRUE.equals(redis.hasKey(PREFIX + jti));
            if (revoked) {
                revokedLocal.put(jti, now.plus(Duration.ofDays(1)));   // Maximum Token Age
                return true;
            }
            okUntil.put(jti, now.plus(recheckAfter));
            if (okUntil.size() > 10_000) {
                okUntil.values().removeIf(t -> !t.isAfter(now));
            }
            return false;
        } catch (RuntimeException e) {
            log.warn("Redis unavailable: revocation check skipped ({})", e.getMessage());
            return false;                                  // Fail-open
        }
    }
}
