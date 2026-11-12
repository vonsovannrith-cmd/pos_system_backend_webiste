package com.example.pos_system.controller;

import com.example.pos_system.dto.user.request.CreateUserRequest;
import com.example.pos_system.dto.user.request.ResetPasswordRequest;
import com.example.pos_system.dto.user.request.UpdateUserRequest;
import com.example.pos_system.dto.user.response.UserAdminResponse;
import com.example.pos_system.model.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<UserAdminResponse> findAll() {
        return userService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserAdminResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @PutMapping("/{id}")
    public UserAdminResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request, Authentication authentication) {
        return userService.update(id, request, authentication.getName());
    }

    @PutMapping("/{id}/reset-password")
    public void resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request);
    }
}
