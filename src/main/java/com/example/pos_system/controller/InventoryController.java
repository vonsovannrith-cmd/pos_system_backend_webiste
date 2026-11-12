package com.example.pos_system.controller;

import com.example.pos_system.dto.request.StockAdjustRequest;
import com.example.pos_system.dto.request.StockInRequest;
import com.example.pos_system.dto.response.PageResponse;
import com.example.pos_system.dto.response.StockMovementResponse;
import com.example.pos_system.model.service.inventory.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/products/{productId}/stock-in")
    public StockMovementResponse stockIn(@PathVariable Long productId, @Valid @RequestBody StockInRequest request, Authentication authentication) {
        return inventoryService.stockIn(productId, request, authentication.getName());
    }

    @PostMapping("/products/{productId}/adjust")
    public StockMovementResponse adjust(@PathVariable Long productId, @Valid @RequestBody StockAdjustRequest request, Authentication authentication) {
        return inventoryService.adjust(productId, request, authentication.getName());
    }

    @GetMapping("/movements")
    public PageResponse<StockMovementResponse> movements(
            @RequestParam(required = false) Long productId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)Pageable pageable)
    {
        return inventoryService.movements(productId, pageable);
    }
}
