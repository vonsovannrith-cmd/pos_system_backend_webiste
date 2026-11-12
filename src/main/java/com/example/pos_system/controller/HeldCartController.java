package com.example.pos_system.controller;

import com.example.pos_system.dto.response.HeldCartResponse;
import com.example.pos_system.dto.response.HeldCartSummary;
import com.example.pos_system.dto.request.HoldCartRequest;
import com.example.pos_system.model.service.HeldCartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/held-carts")
@RequiredArgsConstructor
public class HeldCartController {

    private final HeldCartService heldCartService;

    @GetMapping
    public List<HeldCartSummary> list(Authentication authentication) {
        return heldCartService.list(authentication.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HeldCartSummary hold(@Valid @RequestBody HoldCartRequest request, Authentication authentication) {
        return heldCartService.hold(request, authentication.getName());
    }

    @PostMapping("/{id}/resume")
    public HeldCartResponse resume(@PathVariable Long id, Authentication authentication) {
        return heldCartService.resume(id, authentication.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void discard(@PathVariable Long id, Authentication authentication) {
        heldCartService.discard(id, authentication.getName());
    }
}
