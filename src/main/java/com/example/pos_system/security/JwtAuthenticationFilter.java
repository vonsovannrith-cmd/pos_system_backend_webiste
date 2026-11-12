
package com.example.pos_system.security;

import com.example.pos_system.model.service.auth.AuthCookies;
import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Step 1: Resolve JWT from Header or Cookie
        String token = resolveToken(request);

        // Step 2: No token -> continue without authentication
        if (token == null || token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {

            // Step 3: Extract email from JWT
            String email = jwtService.extractUsername(token);

            // Step 4: Check whether authentication already exists
            if (email != null
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Step 5: Load user from database
                UserDetails user =
                        userDetailsService.loadUserByUsername(email);

                // Step 6: Validate user and JWT
                if (user.isEnabled() && jwtService.isValid(token, user)) {

                    // Step 7: Create Authentication object
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    user,
                                    null,
                                    user.getAuthorities()
                            );

                    auth.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    // Step 8: Save authentication to SecurityContext
                    SecurityContextHolder.getContext()
                            .setAuthentication(auth);
                }
            }

        } catch (
                JwtException
                | IllegalArgumentException
                | UsernameNotFoundException e
        ) {

            // Invalid JWT -> continue without authentication.
            // Spring Security will handle protected endpoints.
            SecurityContextHolder.clearContext();

            log.warn(
                    "JWT authentication failed: {}",
                    e.getClass().getSimpleName()
            );
        }

        // Step 9: Continue filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Resolve JWT from Authorization Header or POS_AUTH Cookie.
     *
     * Priority:
     * 1. Authorization Header
     * 2. POS_AUTH Cookie (only if Header is absent)
     */
    private String resolveToken(HttpServletRequest request) {

        // Check Authorization Header first
        String header = request.getHeader("Authorization");

        if (header != null) {

            if (header.startsWith("Bearer ")) {
                return header.substring(7);
            }

            // Header exists but is invalid -> do not use Cookie
            return null;
        }

        // If Header is absent, check Cookie
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {

            for (Cookie cookie : cookies) {

                if (AuthCookies.NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}
