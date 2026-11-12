package com.example.pos_system.controller;

import com.example.pos_system.dto.auth.ChangePasswordRequest;
import com.example.pos_system.dto.auth.LoginResult;
import com.example.pos_system.dto.request.LoginRequest;
import com.example.pos_system.dto.response.AuthResponse;
import com.example.pos_system.dto.response.UserResponse;
import com.example.pos_system.model.service.auth.AuthCookies;
import com.example.pos_system.model.service.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthCookies authCookies;

    @Value("${app.auth.expose-token:false}")
    private boolean exposeToken;

    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf() {
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {

        LoginResult r = authService.login(request, http.getRemoteAddr());

        AuthResponse body = new AuthResponse(
                exposeToken ? r.token() :  null,
                exposeToken ? "Bearer " : null,
                r.fullName(), r.email(), r.role()
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookies.create(r.token()).toString())
                .body(body);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookies.clear().toString())
                .build();
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) { // This object is automatically retrieved from the SecurityContextHolder (which is populated by the JwtAuthenticationFilter when a user sends a token in the request header).
        if (authentication ==null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "User is not authenticated"
            );
        }
        return authService.me(authentication.getName()); // Generally, it retrieves the username (which, in your code, is the email) of the user who has successfully completed token authentication.
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changPassword(@Valid @RequestBody ChangePasswordRequest request, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "User is not authenticated"
            );
        }
        authService.changePassword(authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }
}
