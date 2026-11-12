package com.example.pos_system.model.service.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AuthCookies {

    public static final String NAME = "POS_AUTH";

    @Value("${app.auth.cookie-secure:false}")
    private boolean secure;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    public ResponseCookie create(String token) {
        return base(token).maxAge(Duration.ofMillis(expirationMs)).build();
    }

    /** Deleting a Cookie: The Path/Attribute must match in order for the browser to delete it */
    public ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)                 // JavaScript not readable
                .secure(secure)                 // Send only over HTTPS (Production)
                .sameSite("Lax")
                .path("/api");                  // Send only with API (not on every page)
    }
}
