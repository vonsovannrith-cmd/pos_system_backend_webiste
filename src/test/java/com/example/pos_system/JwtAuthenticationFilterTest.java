package com.example.pos_system;

import com.example.pos_system.security.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    JwtService jwtService;
    @Mock
    CustomUserDetailsService userDetailsService;
    @Mock
    TokenRevocationStore revocationStore;

    private JwtAuthenticationFilter filter;
    private final AppUserDetails user = new AppUserDetails("a@pos.com", "x", true, "ADMIN", 0);

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(new TokenResolver(), jwtService, userDetailsService, revocationStore);
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
    }

    private Claims claims() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("a@pos.com");
        return claims;
    }

    private MockHttpServletRequest cookieRequest(String token) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setCookies(new Cookie("POS_AUTH", token));
        return req;
    }

    private void run(MockHttpServletRequest request) throws Exception {
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
    }

    private boolean authenticated() {
        return SecurityContextHolder.getContext().getAuthentication() != null;
    }

    @Test
    void validCookieAuthenticates() throws Exception {
        Claims c = claims();
        when(c.getId()).thenReturn("jti-1");
        when(jwtService.parse("good")).thenReturn(c);
        when(userDetailsService.loadUserByUsername("a@pos.com")).thenReturn(user);
        when(jwtService.hasCurrentVersion(c, user)).thenReturn(true);
        when(revocationStore.isRevoked("jti-1")).thenReturn(false);

        run(cookieRequest("good"));

        assertThat(authenticated()).isTrue();
    }

    @Test
    void revokedTokenIsRejected() throws Exception {
        Claims c = claims();
        when(c.getId()).thenReturn("jti-2");
        when(jwtService.parse("loggedout")).thenReturn(c);
        when(userDetailsService.loadUserByUsername("a@pos.com")).thenReturn(user);
        when(jwtService.hasCurrentVersion(c, user)).thenReturn(true);
        when(revocationStore.isRevoked("jti-2")).thenReturn(true);

        run(cookieRequest("loggedout"));

        assertThat(authenticated()).isFalse();
    }

    @Test
    void staleVersionIsRejectedWithoutAskingTheBlocklist() throws Exception {
        Claims c = claims();
        when(jwtService.parse("old")).thenReturn(c);
        when(userDetailsService.loadUserByUsername("a@pos.com")).thenReturn(user);
        when(jwtService.hasCurrentVersion(c, user)).thenReturn(false);   // ប្តូរ Password ក្រោយមក

        run(cookieRequest("old"));

        assertThat(authenticated()).isFalse();
        verify(revocationStore, never()).isRevoked(anyString());
    }

    @Test
    void invalidHeaderIsNotRescuedByValidCookie() throws Exception {
        MockHttpServletRequest req = cookieRequest("good");
        req.addHeader("Authorization", "Bearer bad");
        when(jwtService.parse("bad")).thenThrow(new JwtException("bad token"));

        run(req);

        assertThat(authenticated()).isFalse();
        verify(jwtService, never()).parse("good");            // Cookie មិនត្រូវបានប្រើ
    }

    @Test
    void noCredentialsMeansNoAuthentication() throws Exception {
        run(new MockHttpServletRequest());

        assertThat(authenticated()).isFalse();
        verify(jwtService, never()).parse(anyString());
    }
}
