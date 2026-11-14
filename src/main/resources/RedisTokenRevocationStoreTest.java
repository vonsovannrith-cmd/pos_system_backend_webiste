package com.example.pos_system;

import com.example.pos_system.security.RedisTokenRevocationStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisTokenRevocationStoreTest {

    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> ops;

    private RedisTokenRevocationStore store;

    @BeforeEach
    void setUp() {
        store = new RedisTokenRevocationStore(redis, 60);
    }

    @Test
    void asksRedisOnceThenUsesTheLocalCache() {
        when(redis.hasKey("revoked:j1")).thenReturn(false);

        assertThat(store.isRevoked("j1")).isFalse();
        assertThat(store.isRevoked("j1")).isFalse();

        verify(redis, times(1)).hasKey("revoked:j1");      // សន្សំ Command (Upstash Free មានកំណត់)
    }

    @Test
    void revocationIsImmediateOnTheInstanceThatPerformedIt() {
        when(redis.opsForValue()).thenReturn(ops);

        store.revoke("j2", Instant.now().plusSeconds(600));

        assertThat(store.isRevoked("j2")).isTrue();
        verify(ops).set(eq("revoked:j2"), eq("1"), any(Duration.class));
        verify(redis, never()).hasKey(anyString());
    }

    @Test
    void tokenFoundInRedisIsRevoked() {
        when(redis.hasKey("revoked:j3")).thenReturn(true);

        assertThat(store.isRevoked("j3")).isTrue();
    }

    @Test
    void failsOpenWhenRedisIsDown() {
        when(redis.hasKey("revoked:j4")).thenThrow(new RedisConnectionFailureException("down"));

        assertThat(store.isRevoked("j4")).isFalse();
    }
}
