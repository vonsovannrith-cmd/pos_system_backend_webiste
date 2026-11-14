package com.example.pos_system.security;

import java.time.Instant;

public interface TokenRevocationStore {

    void revoke(String jti, Instant expiresAt);
    boolean isRevoked(String jti);
}
