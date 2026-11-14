package com.backend.aurum.domain.asset.mapper;

import com.backend.aurum.domain.asset.dto.CreateSnapshotDTO;
import com.backend.aurum.domain.asset.dto.SnapshotDTO;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.shared.currency.RateBook;
import com.backend.aurum.domain.shared.exception.AccessDeniedException;
import com.backend.aurum.domain.shared.exception.NotFoundException;
import com.backend.aurum.domain.user.enums.Currency;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SnapshotMapper {

	private final AssetRepository assetRepository;

	public Snapshot toEntity(CreateSnapshotDTO dto, UUID userId) {
		if (dto == null) return null;
		Snapshot snapshot = new Snapshot();
		snapshot.setReferenceDate(dto.getReferenceDate());
		snapshot.setAmountOriginalCurrency(dto.getAmountOriginalCurrency());
		snapshot.setExchangeRateToBase(
			dto.getExchangeRateToBase() != null ? dto.getExchangeRateToBase() : BigDecimal.ONE
		);

		if (dto.getAssetId() != null) {
			Asset asset = assetRepository
				.findById(Objects.requireNonNull(dto.getAssetId()))
				.orElseThrow(() -> new NotFoundException("Asset not found"));

			if (userId != null && !asset.getUser().getId().equals(userId)) {
				throw new AccessDeniedException("Asset does not belong to the user");
			}
			snapshot.setAsset(asset);
		}

		return snapshot;
	}

	public SnapshotDTO toDto(Snapshot entity, RateBook book) {
		if (entity == null) return null;
		Currency currency = currencyOf(entity.getAsset(), book);
		LocalDate referenceDate = entity.getReferenceDate();
		return buildDto(
			entity,
			book.rate(currency, referenceDate),
			book.convert(entity.getAmountOriginalCurrency(), currency, referenceDate)
		);
	}

	private SnapshotDTO buildDto(
		Snapshot entity,
		BigDecimal exchangeRateToBase,
		BigDecimal amountBaseCurrency
	) {
		SnapshotDTO dto = new SnapshotDTO();
		dto.setId(entity.getId());
		dto.setReferenceDate(entity.getReferenceDate());
		dto.setAmountOriginalCurrency(entity.getAmountOriginalCurrency());
		dto.setExchangeRateToBase(exchangeRateToBase);
		dto.setAmountBaseCurrency(amountBaseCurrency);

		if (entity.getAsset() != null) {
			dto.setAssetId(entity.getAsset().getId());
			dto.setAssetName(entity.getAsset().getName());
		}

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
