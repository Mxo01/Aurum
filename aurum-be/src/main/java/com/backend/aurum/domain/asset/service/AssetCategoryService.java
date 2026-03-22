package com.backend.aurum.domain.asset.service;

import com.backend.aurum.domain.asset.model.AssetCategory;
import com.backend.aurum.domain.asset.repository.AssetCategoryRepository;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetCategoryService {

	private final AssetCategoryRepository categoryRepository;

	public List<AssetCategory> findAll(UUID userId) {
		log.debug("AssetCategoryService#findAll - Fetching categories for userId={}", userId);
		List<AssetCategory> categories = categoryRepository.findByUserIdOrUserIsNull(userId);
		log.debug(
			"AssetCategoryService#findAll - Found {} categor(ies) for userId={}",
			categories.size(),
			userId
		);
		return categories;
	}

	public AssetCategory findById(UUID id) {
		log.debug("AssetCategoryService#findById - Looking up categoryId={}", id);
		return categoryRepository
			.findById(Objects.requireNonNull(id))
			.orElseThrow(() -> {
				log.warn("AssetCategoryService#findById - Category not found: categoryId={}", id);
				return new RuntimeException("Category not found");
			});
	}

	@Transactional
	public AssetCategory save(AssetCategory category) {
		log.debug("AssetCategoryService#save - Persisting new category: name={}", category.getName());
		AssetCategory saved = categoryRepository.save(Objects.requireNonNull(category));
		log.info(
			"AssetCategoryService#save - Category created: categoryId={}, name={}",
			saved.getId(),
			saved.getName()
		);
		return saved;
	}

	@Transactional
	public AssetCategory update(UUID id, AssetCategory categoryDetails, UUID requestingUserId) {
		log.debug(
			"AssetCategoryService#update - Updating categoryId={} for userId={}",
			id,
			requestingUserId
		);
		AssetCategory category = findById(id);
		if (category.getUser() == null) {
			log.warn(
				"AssetCategoryService#update - Attempt to modify a default category: categoryId={}",
				id
			);
			throw new ResponseStatusException(
				HttpStatus.FORBIDDEN,
				"Default categories cannot be modified"
			);
		}
		if (!category.getUser().getId().equals(requestingUserId)) {
			log.warn(
				"AssetCategoryService#update - Access denied: categoryId={} belongs to userId={}, requested by userId={}",
				id,
				category.getUser().getId(),
				requestingUserId
			);
			throw new ResponseStatusException(
				HttpStatus.FORBIDDEN,
				"Cannot modify another user's category"
			);
		}
		category.setName(categoryDetails.getName());
		category.setType(categoryDetails.getType());
		category.setIcon(categoryDetails.getIcon());
		AssetCategory updated = categoryRepository.save(category);
		log.info(
			"AssetCategoryService#update - Category updated: categoryId={}, name={}",
			updated.getId(),
			updated.getName()
		);
		return updated;
	}

	@Transactional
	public void delete(UUID id, UUID requestingUserId) {
		log.debug(
			"AssetCategoryService#delete - Deleting categoryId={} for userId={}",
			id,
			requestingUserId
		);
		AssetCategory category = findById(id);
		if (category.getUser() == null) {
			log.warn(
				"AssetCategoryService#delete - Attempt to delete a default category: categoryId={}",
				id
			);
			throw new ResponseStatusException(
				HttpStatus.FORBIDDEN,
				"Default categories cannot be deleted"
			);
		}
		if (!category.getUser().getId().equals(requestingUserId)) {
			log.warn(
				"AssetCategoryService#delete - Access denied: categoryId={} belongs to userId={}, requested by userId={}",
				id,
				category.getUser().getId(),
				requestingUserId
			);
			throw new ResponseStatusException(
				HttpStatus.FORBIDDEN,
				"Cannot delete another user's category"
			);
		}
		categoryRepository.deleteById(Objects.requireNonNull(id));
		log.info("AssetCategoryService#delete - Category deleted: categoryId={}", id);
	}
}
