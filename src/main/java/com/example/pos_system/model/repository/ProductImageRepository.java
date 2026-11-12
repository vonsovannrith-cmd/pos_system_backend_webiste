package com.example.pos_system.model.repository;

import com.example.pos_system.model.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

}
