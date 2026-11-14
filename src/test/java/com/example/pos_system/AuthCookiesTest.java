package com.example.pos_system;

import com.example.pos_system.model.service.auth.AuthCookies;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class AuthCookiesTest {

    private final AuthCookies cookies = new AuthCookies();

    AuthCookiesTest() {
        ReflectionTestUtils.setField(cookies, "secure", true);
        ReflectionTestUtils.setField(cookies, "expirationMs", 43_200_000L);
    }

    @Test
    void create_isHardened() {
        ResponseCookie c = cookies.create("abc");

        assertThat(c.getName()).isEqualTo("POS_AUTH");
        assertThat(c.getValue()).isEqualTo("abc");
        assertThat(c.isHttpOnly()).isTrue();
        assertThat(c.isSecure()).isTrue();
        assertThat(c.getSameSite()).isEqualTo("Lax");
        assertThat(c.getPath()).isEqualTo("/api");
        assertThat(c.getMaxAge()).isEqualTo(Duration.ofHours(12));
    }

    @Test
    void clear_expiresImmediatelyWithSameScope() {
        ResponseCookie c = cookies.clear();

        assertThat(c.getMaxAge()).isEqualTo(Duration.ZERO);
        assertThat(c.getValue()).isEmpty();
        assertThat(c.getPath()).isEqualTo("/api");     // Path ដូចគ្នា ទើប Browser លុបបាន
        assertThat(c.isHttpOnly()).isTrue();
    }
}
