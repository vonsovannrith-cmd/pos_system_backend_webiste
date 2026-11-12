package com.example.pos_system.model.service.user;

import com.example.pos_system.common.BusinessException;
import com.example.pos_system.common.ResourceNotFoundException;
import com.example.pos_system.dto.user.request.CreateUserRequest;
import com.example.pos_system.dto.user.request.ResetPasswordRequest;
import com.example.pos_system.dto.user.request.UpdateUserRequest;
import com.example.pos_system.dto.user.response.UserAdminResponse;
import com.example.pos_system.model.entity.User;
import com.example.pos_system.model.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // find all user by full name
    @Transactional(readOnly = true)
    public List<UserAdminResponse> findAll() {
        return userRepository.findAll(Sort.by("fullName")).stream()
                .map(this::toResponse)
                .toList();
    }

    // create user
    @Transactional
    public UserAdminResponse create(CreateUserRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(
                    "\n" +
                            "This email already exists.", HttpStatus.CONFLICT
            );
        }

        User user = User.builder()
                .fullName(request.fullName().trim())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();
        return toResponse(userRepository.save(user));
    }

    // update user
    @Transactional
    public UserAdminResponse update(Long id, UpdateUserRequest request, String actorEmail) {
        User user = find(id);

        // Prevent an admin from locking themselves out → Ensure there is always at least one admin
        boolean self = user.getEmail().equals(actorEmail);
        if (self && (request.role() != user.getRole() || !user.isActive())) {
            throw new BusinessException(
                    "You cannot deactivate your own account or change your own role.", HttpStatus.CONFLICT
            );
        }

        user.setFullName(request.fullName().trim());
        user.setRole(request.role());
        user.setActive(request.active());
        return toResponse(user); // Dirty Checking
    }

    // reset password
    public void resetPassword(Long id, ResetPasswordRequest request) {
        find(id).setPassword(passwordEncoder.encode(request.newPassword()));
    }

    // find id have in database or not?
    @Transactional
    private User find(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ID is not found"
                ));
    }

    // convert entity to dto response to client
    private UserAdminResponse toResponse(User u) {
        return new UserAdminResponse(
                u.getId(),
                u.getFullName(),
                u.getEmail(),
                u.getRole().name(),
                u.isActive(),
                u.getCreatedAt()
        );
    }
}
