package com.example.pos_system;

import com.example.pos_system.security.InMemoryTokenRevocationStore;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryTokenRevocationStoreTest {

    private final InMemoryTokenRevocationStore store = new InMemoryTokenRevocationStore();

    @Test
    void revokedTokenIsRejectedUntilItExpires() {
        store.revoke("j1", Instant.now().plusSeconds(600));

        assertThat(store.isRevoked("j1")).isTrue();
        assertThat(store.isRevoked("other")).isFalse();
    }

    @Test
    void expiredEntriesAreForgotten() {
        store.revoke("old", Instant.now().minusSeconds(1));

        assertThat(store.isRevoked("old")).isFalse();
    }
}