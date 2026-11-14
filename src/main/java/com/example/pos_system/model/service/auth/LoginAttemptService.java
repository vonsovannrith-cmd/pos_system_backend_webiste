package com.example.pos_system.model.service.auth;


import com.example.pos_system.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;

    private final LoginAttemptStore store;

    public void checkAllowed(String key) {
        if (store.failures(key) >= MAX_ATTEMPTS) {
            throw new BusinessException(
                    "\n" +
                            "Too many failed login attempts. Please wait 15 minutes and try again.",
                    HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    public void recordFailure(String key) {
        store.recordFailure(key);
    }

    public void recordSuccess(String key) {
        store.reset(key);
    }
}
