package com.example.pos_system.controller;

import com.example.pos_system.dto.request.ProductRequest;
import com.example.pos_system.dto.response.PageResponse;
import com.example.pos_system.dto.response.ProductResponse;
import com.example.pos_system.model.service.product.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public PageResponse<ProductResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "false") boolean activeOnly,
            @PageableDefault(size = 10, sort = "name") Pageable pageable
            ) {
        return productService.search(keyword, categoryId, activeOnly, pageable);
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public List<ProductResponse> lowStock() {
        return productService.lowStock();
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return productService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ProductResponse setActive(@PathVariable Long id, @RequestParam boolean value) {
        return productService.setActive(id, value);
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ProductResponse uploadImage(@PathVariable Long id, @RequestParam("file")MultipartFile file) {
        return productService.uploadImage(id, file);
    }

    @DeleteMapping("/{id}/image")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ProductResponse removeImage(@PathVariable Long id) {
        return productService.removeImage(id);
    }
}
