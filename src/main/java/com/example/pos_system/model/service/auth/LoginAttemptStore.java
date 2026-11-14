package com.example.pos_system.model.service.auth;

import java.time.Duration;

public interface LoginAttemptStore {


    Duration WINDOW = Duration.ofMinutes(15);

    /** Number of failures in the current window (0 if none/expired) */
    int failures(String key);

    void recordFailure(String key);

    void reset(String key);
}
