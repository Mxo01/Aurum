package com.backend.aurum.domain.asset.mapper;

import com.backend.aurum.domain.asset.dto.AssetDTO;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.AssetCategory;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.AssetCategoryRepository;
import com.backend.aurum.domain.user.enums.Currency;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AssetMapper {

	private final AssetCategoryRepository categoryRepository;
	private final UserRepository userRepository;
	private final SnapshotMapper snapshotMapper;

	public Asset toEntity(AssetDTO dto, UUID userId) {
		if (dto == null) return null;
		Asset asset = new Asset();
		asset.setName(dto.getName());
		asset.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
		asset.setIsFavorite(dto.getIsFavorite() != null ? dto.getIsFavorite() : false);
		asset.setOriginalCurrency(
			dto.getOriginalCurrency() != null ? dto.getOriginalCurrency() : Currency.EUR
		);
		asset.setLiabilityType(dto.getLiabilityType());
		asset.setPaymentFrequency(dto.getPaymentFrequency());
		asset.setPaymentAmount(dto.getPaymentAmount());

		if (userId != null) {
			User user = userRepository
				.findById(userId)
				.orElseThrow(() -> new RuntimeException("User not found"));
			asset.setUser(user);
		}

		if (dto.getCategoryId() != null) {
			AssetCategory category = categoryRepository
				.findById(Objects.requireNonNull(dto.getCategoryId()))
				.orElseThrow(() -> new RuntimeException("Category not found"));

			if (
				category.getUser() != null && (userId == null || !category.getUser().getId().equals(userId))
			) {
				throw new RuntimeException("Category does not belong to the user");
			}

			asset.setCategory(category);
		}

		return asset;
	}

	public AssetDTO toDto(Asset entity) {
		if (entity == null) return null;
		AssetDTO dto = toDtoLight(entity, List.of());

		if (entity.getSnapshots() != null) {
			dto.setSnapshots(entity.getSnapshots().stream().map(snapshotMapper::toDto).toList());
		}

		return dto;
	}

	/**
	 * Light DTO for list responses — no snapshots array. Accepts the latest 2 snapshots
	 * (ordered desc by referenceDate) to compute latestValue, latestValueBase, previousValue.
	 */
	public AssetDTO toDtoLight(Asset entity, List<Snapshot> latestTwo) {
		if (entity == null) return null;
		AssetDTO dto = new AssetDTO();
		dto.setId(entity.getId());
		dto.setName(entity.getName());
		dto.setIsActive(entity.getIsActive());
		dto.setIsFavorite(entity.getIsFavorite());
		dto.setOriginalCurrency(entity.getOriginalCurrency());

		if (entity.getCategory() != null) {
			dto.setCategoryId(entity.getCategory().getId());
			dto.setCategoryName(entity.getCategory().getName());
			dto.setCategoryIcon(entity.getCategory().getIcon());
			dto.setType(entity.getCategory().getType());
		}

		if (!latestTwo.isEmpty()) {
			Snapshot latest = latestTwo.get(0);
			dto.setLatestValue(latest.getAmountOriginalCurrency());
			dto.setLatestValueBase(latest.getAmountInBaseCurrency());
			if (latestTwo.size() > 1) {
				dto.setPreviousValue(latestTwo.get(1).getAmountOriginalCurrency());
			}
		}

		dto.setLiabilityType(entity.getLiabilityType());
		dto.setPaymentFrequency(entity.getPaymentFrequency());
		dto.setPaymentAmount(entity.getPaymentAmount());

		return dto;
	}

	public AssetDTO toDtoLight(Asset entity, Map<UUID, List<Snapshot>> latestTwoByAssetId) {
		return toDtoLight(entity, latestTwoByAssetId.getOrDefault(entity.getId(), List.of()));
	}
}
