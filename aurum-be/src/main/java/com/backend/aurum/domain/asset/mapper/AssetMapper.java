package com.backend.aurum.domain.asset.mapper;

import com.backend.aurum.domain.asset.dto.AssetDTO;
import com.backend.aurum.domain.asset.dto.CreateAssetDTO;
import com.backend.aurum.domain.asset.dto.UpdateAssetDTO;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.AssetCategory;
import com.backend.aurum.domain.asset.model.AssetUpdate;
import com.backend.aurum.domain.asset.model.LiabilityConfiguration;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.AssetCategoryRepository;
import com.backend.aurum.domain.shared.currency.RateBook;
import com.backend.aurum.domain.shared.exception.AccessDeniedException;
import com.backend.aurum.domain.shared.exception.NotFoundException;
import com.backend.aurum.domain.user.enums.Currency;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AssetMapper {

	private final AssetCategoryRepository categoryRepository;
	private final UserRepository userRepository;
	private final SnapshotMapper snapshotMapper;

	public Asset toEntity(CreateAssetDTO dto, UUID userId) {
		if (dto == null) return null;
		Asset asset = new Asset();
		asset.setName(dto.getName());
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
				.orElseThrow(() -> new NotFoundException("User not found"));
			asset.setUser(user);
		}

		if (dto.getCategoryId() != null) {
			asset.setCategory(resolveCategory(dto.getCategoryId(), userId));
		}

		return asset;
	}

	public AssetUpdate toUpdate(UpdateAssetDTO dto, UUID userId) {
		if (dto == null) return null;
		Optional<AssetCategory> category =
			dto.getCategoryId() == null
				? Optional.empty()
				: Optional.of(resolveCategory(dto.getCategoryId(), userId));
		return new AssetUpdate(
			dto.getName(),
			category,
			dto.getOriginalCurrency(),
			dto.getIsFavorite(),
			liabilityOf(dto)
		);
	}

	/**
	 * Any liability field in the body selects "replace the block"; the rule object then rejects a
	 * frequency or an amount that arrives without its liabilityType instead of dropping it.
	 */
	private Optional<LiabilityConfiguration> liabilityOf(UpdateAssetDTO dto) {
		if (
			!dto.isLiabilityTypeProvided() &&
			dto.getPaymentFrequency() == null &&
			dto.getPaymentAmount() == null
		) {
			return Optional.empty();
		}

		return Optional.of(
			new LiabilityConfiguration(
				dto.getLiabilityType(),
				dto.getPaymentFrequency(),
				dto.getPaymentAmount()
			)
		);
	}

	private AssetCategory resolveCategory(UUID categoryId, UUID userId) {
		AssetCategory category = categoryRepository
			.findById(Objects.requireNonNull(categoryId))
			.orElseThrow(() -> new NotFoundException("Category not found"));

		if (
			category.getUser() != null && (userId == null || !category.getUser().getId().equals(userId))
		) {
			throw new AccessDeniedException("Category does not belong to the user");
		}

		return category;
	}

	public AssetDTO toDto(Asset entity, RateBook book) {
		if (entity == null) return null;
		AssetDTO dto = buildDto(entity);

		if (entity.getSnapshots() != null) {
			dto.setSnapshots(
				entity
					.getSnapshots()
					.stream()
					.map(snapshot -> snapshotMapper.toDto(snapshot, book))
					.toList()
			);
		}

		return dto;
	}

	/**
	 * Light DTO for list responses — no snapshots array. Accepts the latest 2 snapshots
	 * (ordered desc by referenceDate) to compute latestValue, latestValueBase, previousValue.
	 */
	public AssetDTO toDtoLight(
		Asset entity,
		List<Snapshot> latestTwo,
		RateBook book,
		LocalDate asOf
	) {
		return toDtoLight(entity, latestTwo, (asset, latest) ->
			book.convert(latest.getAmountOriginalCurrency(), currencyOf(asset, book), asOf)
		);
	}

	private AssetDTO toDtoLight(
		Asset entity,
		List<Snapshot> latestTwo,
		BiFunction<Asset, Snapshot, BigDecimal> latestValueBase
	) {
		if (entity == null) return null;
		AssetDTO dto = buildDto(entity);

		if (!latestTwo.isEmpty()) {
			Snapshot latest = latestTwo.get(0);
			dto.setLatestValue(latest.getAmountOriginalCurrency());
			dto.setLatestValueBase(latestValueBase.apply(entity, latest));
			if (latestTwo.size() > 1) {
				dto.setPreviousValue(latestTwo.get(1).getAmountOriginalCurrency());
			}
		}

		return dto;
	}

	private AssetDTO buildDto(Asset entity) {
		AssetDTO dto = new AssetDTO();
		dto.setId(entity.getId());
		dto.setName(entity.getName());
		dto.setIsFavorite(entity.getIsFavorite());
		dto.setOriginalCurrency(entity.getOriginalCurrency());

		if (entity.getCategory() != null) {
			dto.setCategoryId(entity.getCategory().getId());
			dto.setCategoryName(entity.getCategory().getName());
			dto.setCategoryIcon(entity.getCategory().getIcon());
			dto.setType(entity.getCategory().getType());
		}

		dto.setLiabilityType(entity.getLiabilityType());
		dto.setPaymentFrequency(entity.getPaymentFrequency());
		dto.setPaymentAmount(entity.getPaymentAmount());

		return dto;
	}

	/**
	 * Legacy rows can carry no currency: they were written in the user's currency,
	 * which is the book's target.
	 */
	private Currency currencyOf(Asset asset, RateBook book) {
		return asset.getOriginalCurrency() != null ? asset.getOriginalCurrency() : book.target();
	}
}
