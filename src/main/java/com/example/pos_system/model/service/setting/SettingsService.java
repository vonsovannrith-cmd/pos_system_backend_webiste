package com.example.pos_system.model.service.setting;

import com.example.pos_system.dto.request.SettingsRequest;
import com.example.pos_system.dto.response.SettingsResponse;
import com.example.pos_system.model.entity.Settings;
import com.example.pos_system.model.repository.setting.SettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final SettingsRepository settingsRepository;

    /** Used by various services (OrderService, ProductService) */
    @Transactional(readOnly = true)
    public Settings current() {
        return settingsRepository.findById(Settings.ID)
                .orElseThrow(() -> new IllegalStateException("Settings row is missing (V5 migration)"));
    }

    public SettingsResponse get() {
        return toResponse(current());
    }

    public SettingsResponse update(SettingsRequest request, String email) {
        Settings settings = current();
        settings.setStoreName(request.storeName().trim());
        settings.setAddress(request.address());
        settings.setPhone(request.phone());
        settings.setTaxRate(request.taxRate());
        settings.setUsdToKhr(request.usdToKhr());
        settings.setDefaultLowStock(request.defaultLowStockThreshold());
        settings.setUpdatedAt(LocalDateTime.now());
        settings.setUpdatedBy(email);
        return toResponse(settings); // Dirty checking → UPDATE when Commit
    }

    private String blankToNull(String v) {
        return  (v == null || v.isBlank()) ? null : v.trim();
    }

    private SettingsResponse toResponse(Settings s) {
        return new SettingsResponse(
                s.getStoreName(),
                s.getAddress(),
                s.getPhone(),
                s.getTaxRate(),
                s.getUsdToKhr(),
                s.getDefaultLowStock()
        );
    }
}
