package com.backend.aurum.domain.asset.service;

import com.backend.aurum.domain.asset.model.AssetCategory;
import com.backend.aurum.domain.asset.repository.AssetCategoryRepository;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AssetCategoryService {

	private final AssetCategoryRepository categoryRepository;

	public List<AssetCategory> findAll(UUID userId) {
		return categoryRepository.findByUserIdOrUserIsNull(userId);
	}

	public AssetCategory findById(UUID id) {
		return categoryRepository
			.findById(Objects.requireNonNull(id))
			.orElseThrow(() -> new RuntimeException("Category not found"));
	}

	@Transactional
	public AssetCategory save(AssetCategory category) {
		return categoryRepository.save(Objects.requireNonNull(category));
	}

	@Transactional
	public AssetCategory update(UUID id, AssetCategory categoryDetails, UUID requestingUserId) {
		AssetCategory category = findById(id);
		if (category.getUser() == null) {
			throw new ResponseStatusException(
				HttpStatus.FORBIDDEN,
				"Default categories cannot be modified"
			);
		}
		if (!category.getUser().getId().equals(requestingUserId)) {
			throw new ResponseStatusException(
				HttpStatus.FORBIDDEN,
				"Cannot modify another user's category"
			);
		}
		category.setName(categoryDetails.getName());
		category.setType(categoryDetails.getType());
		category.setIcon(categoryDetails.getIcon());
		return categoryRepository.save(category);
	}

	@Transactional
	public void delete(UUID id, UUID requestingUserId) {
		AssetCategory category = findById(id);
		if (category.getUser() == null) {
			throw new ResponseStatusException(
				HttpStatus.FORBIDDEN,
				"Default categories cannot be deleted"
			);
		}
		if (!category.getUser().getId().equals(requestingUserId)) {
			throw new ResponseStatusException(
				HttpStatus.FORBIDDEN,
				"Cannot delete another user's category"
			);
		}
		categoryRepository.deleteById(Objects.requireNonNull(id));
	}
}
