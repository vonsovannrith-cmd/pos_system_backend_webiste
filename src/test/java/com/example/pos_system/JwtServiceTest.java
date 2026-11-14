package com.example.pos_system;

import com.example.pos_system.security.AppUserDetails;
import com.example.pos_system.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private final JwtService jwt = new JwtService();

    JwtServiceTest() {
        ReflectionTestUtils.setField(jwt, "secret", Base64.getEncoder()
                .encodeToString("unit-test-secret-key-0123456789-abcdef".getBytes(StandardCharsets.UTF_8)));
        ReflectionTestUtils.setField(jwt, "expirationMs", 60_000L);
    }

    private AppUserDetails user(int version) {
        return new AppUserDetails("a@pos.com", "x", true, "ADMIN", version);
    }

    @Test
    void tokenCarriesSubjectRoleVersionAndUniqueId() {
        Claims c = jwt.parse(jwt.generateToken(user(3)));

        assertThat(c.getSubject()).isEqualTo("a@pos.com");
        assertThat(c.get("role", String.class)).isEqualTo("ROLE_ADMIN");
        assertThat(c.get("ver", Integer.class)).isEqualTo(3);
        assertThat(c.getId()).isNotBlank();
    }

    @Test
    void everyTokenHasItsOwnJti() {
        String a = jwt.parse(jwt.generateToken(user(0))).getId();
        String b = jwt.parse(jwt.generateToken(user(0))).getId();

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void versionMismatchMakesTheTokenStale() {
        Claims c = jwt.parse(jwt.generateToken(user(3)));

        assertThat(jwt.hasCurrentVersion(c, user(3))).isTrue();
        assertThat(jwt.hasCurrentVersion(c, user(4))).isFalse();   // ប្តូរ Password/Role ក្រោយមក
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwt.generateToken(user(0));
        String tampered = token.substring(0, token.length() - 3) + "abc";

        assertThatThrownBy(() -> jwt.parse(tampered)).isInstanceOf(JwtException.class);
    }
}
