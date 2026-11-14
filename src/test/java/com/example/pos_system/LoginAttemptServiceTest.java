package com.example.pos_system;

import com.example.pos_system.common.BusinessException;
import com.example.pos_system.model.service.auth.InMemoryLoginAttemptStore;
import com.example.pos_system.model.service.auth.LoginAttemptService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptServiceTest {

    private final LoginAttemptService service = new LoginAttemptService(new InMemoryLoginAttemptStore());

    @Test
    void blocksAfterFiveFailures() {
        for (int i = 0; i < 5; i++) {
            service.checkAllowed("ip|a@pos.com");
            service.recordFailure("ip|a@pos.com");
        }
        assertThatThrownBy(() -> service.checkAllowed("ip|a@pos.com"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void successResetsTheCounter() {
        for (int i = 0; i < 4; i++) service.recordFailure("k");
        service.recordSuccess("k");
        for (int i = 0; i < 4; i++) service.recordFailure("k");

        assertThatCode(() -> service.checkAllowed("k")).doesNotThrowAnyException();
    }

    @Test
    void differentKeysAreIndependent() {
        for (int i = 0; i < 5; i++) service.recordFailure("ip1|a@pos.com");

        assertThatCode(() -> service.checkAllowed("ip2|a@pos.com")).doesNotThrowAnyException();
    }
}
