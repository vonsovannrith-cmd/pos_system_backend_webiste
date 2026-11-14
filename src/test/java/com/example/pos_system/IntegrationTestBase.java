
package com.example.pos_system;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTestBase {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {

        // Secret for testing purposes only (at least 256 bits)
        registry.add(
                "app.jwt.secret",
                () -> Base64.getEncoder().encodeToString(
                        "integration-test-secret-key-0123456789"
                                .getBytes(StandardCharsets.UTF_8)
                )
        );
        registry.add("app.redis.enabled", () -> "true");

        // Expose token in test environment
        registry.add("app.auth.expose-token", () -> "true");
    }
}
