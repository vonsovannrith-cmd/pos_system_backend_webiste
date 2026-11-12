package com.example.pos_system.model.service.auth;

import com.example.pos_system.common.BusinessException;
import com.example.pos_system.dto.auth.ChangePasswordRequest;
import com.example.pos_system.dto.auth.LoginResult;
import com.example.pos_system.dto.request.LoginRequest;
import com.example.pos_system.dto.response.AuthResponse;
import com.example.pos_system.dto.response.UserResponse;
import com.example.pos_system.model.entity.User;
import com.example.pos_system.model.repository.UserRepository;
import com.example.pos_system.security.JwtService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
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

    // login method
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

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();

        String token = jwtService.generateToken(userDetails);
        return new LoginResult(
                token,
                user.getFullName(),
                user.getEmail(),
                user.getRole().name());
    }

    // information me
    public UserResponse me(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        return new UserResponse(user.getId(), user.getFullName(),
                user.getEmail(), user.getRole().name());
    }

    // change password
    @Transactional
    public void changePassword(String email, ChangePasswordRequest req) {
        User user = userRepository.findByEmail(email).orElseThrow();

        if (!passwordEncoder.matches(req.currentPassword(), user.getPassword())) {
            throw new BusinessException("Current password is Invalid", HttpStatus.BAD_REQUEST);   // 400 no 401
        }
        if (passwordEncoder.matches(req.newPassword(), user.getPassword())) {
            throw new BusinessException("The new password must be different from the old password.", HttpStatus.BAD_REQUEST);
        }
        user.setPassword(passwordEncoder.encode(req.newPassword()));
    }
}
