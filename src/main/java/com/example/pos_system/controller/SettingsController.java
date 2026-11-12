package com.example.pos_system.controller;

import com.example.pos_system.dto.request.SettingsRequest;
import com.example.pos_system.dto.response.SettingsResponse;
import com.example.pos_system.model.service.setting.SettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    // Everyone who logs in (POS requires VAT and store name)
    @GetMapping
    public SettingsResponse get() {
        return settingsService.get();
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public SettingsResponse update(@Valid @RequestBody SettingsRequest request, Authentication authentication) {
        return settingsService.update(request, authentication.getName());
    }
}
