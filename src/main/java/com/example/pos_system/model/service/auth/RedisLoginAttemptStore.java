package com.example.pos_system.model.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true")
public class RedisLoginAttemptStore implements LoginAttemptStore {

    private static final String PREFIX = "login:fail:";

    private final StringRedisTemplate redis;

    @Override
    public int failures(String key) {
        try {
            String value = redis.opsForValue().get(redisKey(key));
            return value == null ? 0 : Integer.parseInt(value);
        } catch (RuntimeException e) {
            log.warn("Redis unavailable (login limiter): {}", e.getMessage());
            return 0;                                     // Fail-open
        }
    }

    @Override
    public void recordFailure(String key) {
        try {
            String k = redisKey(key);
            // SET NX + EX: Create with a TTL (a key never exists without a TTL), and INCR preserves the TTL.
            redis.opsForValue().setIfAbsent(k, "0", WINDOW);
            redis.opsForValue().increment(k);
        } catch (RuntimeException e) {
            log.warn("Redis unavailable (login limiter): {}", e.getMessage());
        }
    }

    @Override
    public void reset(String key) {
        try {
            redis.delete(redisKey(key));
        } catch (RuntimeException e) {
            log.warn("Redis unavailable (login limiter): {}", e.getMessage());
        }
    }

    /** Hash to prevent long/unusual emails (entered by an attacker) from becoming large keys */
    private String redisKey(String key) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
            return PREFIX + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}