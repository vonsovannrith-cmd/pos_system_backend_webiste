package com.example.pos_system.model.service.product;

import com.example.pos_system.common.BusinessException;
import com.example.pos_system.common.ResourceNotFoundException;
import com.example.pos_system.dto.request.ProductRequest;
import com.example.pos_system.dto.response.PageResponse;
import com.example.pos_system.dto.response.ProductResponse;
import com.example.pos_system.model.entity.Category;
import com.example.pos_system.model.entity.Product;
import com.example.pos_system.model.entity.ProductImage;
import com.example.pos_system.model.repository.category.CategoryRepository;
import com.example.pos_system.model.repository.product.ProductRepository;
import com.example.pos_system.model.service.ImageService;
import com.example.pos_system.model.service.setting.SettingsService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SettingsService settingsService;
    private final ImageService imageService;

    // Dynamic search
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(String keyword, Long categoryId,
                                                boolean activeOnly, Pageable pageable) {
        Specification<Product> spec = buildSpec(keyword, categoryId, activeOnly);
        return PageResponse.from(productRepository.findAll(spec, pageable)
                .map(this::toResponse));
    }

    // Return product by id
    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        return toResponse(find(id));
    }

    // show data low stock
    @Transactional(readOnly = true)
    public List<ProductResponse> lowStock() {
        return productRepository.findLowStock()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // create product
    @Transactional
    public ProductResponse create(ProductRequest request) {
        String sku = normalizeSku(request.sku());
        String barcode = normalizeBlank(request.barCode());

        if (productRepository.existsBySku(sku)) {
            throw new BusinessException("\n" +
                    "This SKU already exists.", HttpStatus.CONFLICT);
        }
        if (barcode != null && productRepository.existsByBarcode(barcode)) {
            throw new BusinessException("\n" +
                    "This Barcode already exists.", HttpStatus.CONFLICT);
        }

        Product product = new Product();
        product.setLowStockThreshold(settingsService.current().getDefaultLowStock());
        apply(product, request, sku, barcode);
        return toResponse(productRepository.save(product));
    }

    // update product
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = find(id);
        String sku = normalizeSku(request.sku());
        String barcode = normalizeBlank(request.barCode());

        if (productRepository.existsBySkuAndIdNot(sku, id)) {
            throw new BusinessException("This SKU already exists.", HttpStatus.CONFLICT);
        }

        if (barcode != null && productRepository.existsByBarcodeAndIdNot(barcode, id)) {
            throw new BusinessException("This Barcode already exists.", HttpStatus.CONFLICT);
        }
        apply(product, request, sku, barcode);
        return toResponse(product);
    }

    // set Active and Inactive
    @Transactional
    public ProductResponse setActive(Long id, boolean active) {
        Product product = find(id);
        product.setActive(active);
        return toResponse(product);
    }

    @Transactional
    public ProductResponse uploadImage(Long id, MultipartFile file) {
        Product product = find(id);     // Check existing stock
        ProductImage image = imageService.store(file);  // // Review + Compress + Save
        UUID oldId = product.getImageId();
        product.setImageId(image.getId());
        if (oldId != null) {
            imageService.delete(oldId);     // Delete old image (do not leave orphans)
        }
        return toResponse(product);
    }

    @Transactional
    public ProductResponse removeImage(Long id) {
        Product product = find(id);
        UUID oldId = product.getImageId();
        if (oldId == null) {
            product.setImageId(null);
            imageService.delete(oldId);
        }
        return toResponse(product);
    }

    // ----------------- helpers -----------------

    // Role: Create criteria (predicates) for use with Spring Data JPA Specification to search for products based on Frontend requirements.
    private Specification<Product> buildSpec(String keyword, Long categoryId, boolean activeOnly) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String text = keyword.trim();
                String like = "%" + text.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("sku")), like),
                        cb.equal(root.get("barcode"), text)
                ));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (activeOnly) {
                predicates.add(cb.isTrue(root.get("active")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // Define data Product Entity by Product Request
    private void apply(Product p, ProductRequest request, String sku, String barcode) {
        p.setName(request.name().trim());
        p.setSku(request.sku());
        p.setBarcode(request.barCode());
        p.setPrice(request.price());
        p.setCostPrice(request.costPrice() != null ? request.costPrice() : BigDecimal.ZERO);
        if (request.lowStockThreshold() != null) {
            p.setLowStockThreshold(request.lowStockThreshold());
        }
        if (request.active() != null) {
            p.setActive(request.active());
        }
        if (request.categoryId() == null) {
            p.setCategory(null);
        }else {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "CategoryId not found"
                    ));
            p.setCategory(category);
        }
    }

    // find product id
    private Product find(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found"
                ));
    }

    // convert sku to toUpperCase
    private String normalizeSku(String sku) {
        return sku.trim().toUpperCase();
    }

    // check value (null or blank) or not after it convert to null save in database
    private String normalizeBlank(String value) {
        return (value == null || value.isBlank() ? null : value.trim());
    }

    // convert entity to dto response to client
    public ProductResponse toResponse(Product p) {
        Category c = p.getCategory();
        return new ProductResponse(
                p.getId(),
                p.getName(),
                p.getSku(),
                p.getBarcode(),
                p.getPrice(),
                p.getCostPrice(),
                p.getStockQuantity(),
                p.getLowStockThreshold(),
                p.getStockQuantity() <= p.getLowStockThreshold(),
                p.isActive(),
                c != null ? c.getId() : null,
                c != null ? c.getName() : null,
                p.getImageId() != null ? "/api/images/" + p.getImageId() : null
        );

    }
}
