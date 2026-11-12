package com.example.pos_system.model.service.cateogry;

import com.example.pos_system.common.BusinessException;
import com.example.pos_system.common.ResourceNotFoundException;
import com.example.pos_system.dto.request.CategoryRequest;
import com.example.pos_system.dto.response.CategoryResponse;
import com.example.pos_system.model.entity.Category;
import com.example.pos_system.model.repository.category.CategoryRepository;
import com.example.pos_system.model.repository.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll(Sort.by("name")).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException("This category name already exists.", HttpStatus.CONFLICT);
        }
        Category category = new Category();
        category.setName(name);
        category.setDescription(request.description());
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = find(id);
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BusinessException("This category name already exists.", HttpStatus.CONFLICT);
        }
        category.setName(name);
        category.setDescription(request.description());
        return toResponse(category);    // No need to call save() because the entity is "tracked"
    }

    @Transactional
    public void delete(Long id) {
        Category category= find(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new BusinessException(
                    "\n" +
                            "Cannot be deleted: This category contains items.", HttpStatus.CONFLICT
            );
        }
        categoryRepository.delete(category);
    }

    private Category find(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found"
                ));

    }

    private CategoryResponse toResponse(Category c) {
        return new CategoryResponse(
                c.getId(),
                c.getName(),
                c.getDescription()
        );
    }
}
