package com.example.pos_system.model.service.auth;

import com.example.pos_system.common.BusinessException;
import com.example.pos_system.dto.auth.ChangePasswordRequest;
import com.example.pos_system.dto.auth.LoginResult;
import com.example.pos_system.dto.request.LoginRequest;
import com.example.pos_system.dto.response.UserResponse;
import com.example.pos_system.model.entity.User;
import com.example.pos_system.model.repository.UserRepository;
import com.example.pos_system.security.AppUserDetails;
import com.example.pos_system.security.JwtService;
import com.example.pos_system.security.TokenRevocationStore;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final TokenRevocationStore revocationStore;

    public LoginResult login(LoginRequest request, String clientIp) {
        String email = request.email().trim().toLowerCase();
        String key = clientIp + "|" + email;

        loginAttemptService.checkAllowed(key);

        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (BadCredentialsException e) {
            loginAttemptService.recordFailure(key);
            throw e;
        }
        loginAttemptService.recordSuccess(key);

        AppUserDetails details = (AppUserDetails) auth.getPrincipal();
        User user = userRepository.findByEmail(details.getUsername()).orElseThrow();

        return new LoginResult(jwtService.generateToken(details),
                user.getFullName(), user.getEmail(), user.getRole().name());
    }

    public UserResponse me(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole().name());
    }

    /** Change Password → All old tokens become invalid. Return a new token for the current session. */
    @Transactional
    public String changePassword(String email, ChangePasswordRequest req) {
        User user = userRepository.findByEmail(email).orElseThrow();

        if (!passwordEncoder.matches(req.currentPassword(), user.getPassword())) {
            throw new BusinessException("Current password is incorrect", HttpStatus.BAD_REQUEST);
        }
        if (passwordEncoder.matches(req.newPassword(), user.getPassword())) {
            throw new BusinessException("New password must be different from the old password", HttpStatus.BAD_REQUEST);
        }

        user.setPassword(passwordEncoder.encode(req.newPassword()));
        user.setTokenVersion(user.getTokenVersion() + 1);

        return jwtService.generateToken(new AppUserDetails(user.getEmail(), user.getPassword(),
                user.isActive(), user.getRole().name(), user.getTokenVersion()));
    }

    /** Logout this device: Add the jti to the blocklist until the token expires */
    public void logout(String token) {
        if (token == null) return;
        try {
            Claims claims = jwtService.parse(token);
            revocationStore.revoke(claims.getId(), claims.getExpiration().toInstant());
        } catch (JwtException | IllegalArgumentException ignored) {
            // Token is invalid/expired; nothing to revoke.
        }
    }
}
