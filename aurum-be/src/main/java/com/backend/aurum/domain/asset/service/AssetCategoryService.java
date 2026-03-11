package com.backend.aurum.domain.asset.service;

import com.backend.aurum.domain.asset.model.AssetCategory;
import com.backend.aurum.domain.asset.repository.AssetCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssetCategoryService {

    private final AssetCategoryRepository categoryRepository;

    public List<AssetCategory> findAll(UUID userId) {
        return categoryRepository.findByUserIdOrUserIsNull(userId);
    }

    public AssetCategory findById(UUID id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Category not found"));
    }

    @Transactional
    public AssetCategory save(AssetCategory category) {
        return categoryRepository.save(category);
    }

    @Transactional
    public AssetCategory update(UUID id, AssetCategory categoryDetails) {
        AssetCategory category = findById(id);
        category.setName(categoryDetails.getName());
        category.setType(categoryDetails.getType());
        category.setUser(categoryDetails.getUser());
        return categoryRepository.save(category);
    }

    @Transactional
    public void delete(UUID id) {
        categoryRepository.deleteById(id);
    }
}
