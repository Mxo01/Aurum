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
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AssetMapper {

	private final AssetCategoryRepository categoryRepository;
	private final UserRepository userRepository;
	private final SnapshotMapper snapshotMapper;

	public Asset toEntity(CreateAssetDTO dto, UUID userId) {
		Objects.requireNonNull(userId, "userId");
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

		User user = userRepository
			.findById(userId)
			.orElseThrow(() -> new NotFoundException("User not found"));
		asset.setUser(user);

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
		Objects.requireNonNull(userId, "userId");
		AssetCategory category = categoryRepository
			.findById(Objects.requireNonNull(categoryId))
			.orElseThrow(() -> new NotFoundException("Category not found"));

		if (category.getUser() != null && !category.getUser().getId().equals(userId)) {
			throw new AccessDeniedException("This category does not belong to you");
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
	 * Every amount is converted from the currency its own snapshot was written in, so an asset
	 * whose currency changed since does not restate the past.
	 */
	public AssetDTO toDtoLight(
		Asset entity,
		List<Snapshot> latestTwo,
		RateBook book,
		LocalDate asOf
	) {
		if (entity == null) return null;
		AssetDTO dto = buildDto(entity);

		if (!latestTwo.isEmpty()) {
			Snapshot latest = latestTwo.get(0);
			Currency latestCurrency = currencyOf(latest, book);
			dto.setLatestValue(latest.getAmountOriginalCurrency());
			dto.setLatestValueCurrency(latestCurrency);
			dto.setLatestValueBase(
				book.convert(latest.getAmountOriginalCurrency(), latestCurrency, asOf)
			);
			if (latestTwo.size() > 1) {
				Snapshot previous = latestTwo.get(1);
				dto.setPreviousValue(previous.getAmountOriginalCurrency());
				dto.setPreviousValueCurrency(currencyOf(previous, book));
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
	 * The row's own currency, never the asset's current one. The book target is the last resort of a
	 * row built in memory with no asset at all.
	 */
	private Currency currencyOf(Snapshot snapshot, RateBook book) {
		Currency currency = snapshot.resolveCurrency();
		return currency != null ? currency : book.target();
	}
}
