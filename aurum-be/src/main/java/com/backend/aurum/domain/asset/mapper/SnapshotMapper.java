package com.backend.aurum.domain.asset.mapper;

import com.backend.aurum.domain.asset.dto.CreateSnapshotDTO;
import com.backend.aurum.domain.asset.dto.SnapshotDTO;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.model.SnapshotSource;
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
		Objects.requireNonNull(userId, "userId");
		if (dto == null) return null;
		Snapshot snapshot = new Snapshot();
		snapshot.setReferenceDate(dto.getReferenceDate());
		snapshot.setAmountOriginalCurrency(dto.getAmountOriginalCurrency());
		snapshot.setExchangeRateToBase(
			dto.getExchangeRateToBase() != null ? dto.getExchangeRateToBase() : BigDecimal.ONE
		);
		snapshot.setSource(SnapshotSource.USER);

		if (dto.getAssetId() != null) {
			Asset asset = assetRepository
				.findById(Objects.requireNonNull(dto.getAssetId()))
				.orElseThrow(() -> new NotFoundException("Asset not found"));

			if (!asset.getUser().getId().equals(userId)) {
				throw new AccessDeniedException("This asset does not belong to you");
			}
			snapshot.setAsset(asset);
			snapshot.setCurrency(asset.getOriginalCurrency());
		}

		return snapshot;
	}

	public SnapshotDTO toDto(Snapshot entity, RateBook book) {
		if (entity == null) return null;
		Currency currency = currencyOf(entity, book);
		LocalDate referenceDate = entity.getReferenceDate();
		return buildDto(
			entity,
			currency,
			book.rate(currency, referenceDate),
			book.convert(entity.getAmountOriginalCurrency(), currency, referenceDate)
		);
	}

	private SnapshotDTO buildDto(
		Snapshot entity,
		Currency currency,
		BigDecimal exchangeRateToBase,
		BigDecimal amountBaseCurrency
	) {
		SnapshotDTO dto = new SnapshotDTO();
		dto.setId(entity.getId());
		dto.setReferenceDate(entity.getReferenceDate());
		dto.setAmountOriginalCurrency(entity.getAmountOriginalCurrency());
		dto.setCurrency(currency);
		dto.setExchangeRateToBase(exchangeRateToBase);
		dto.setAmountBaseCurrency(amountBaseCurrency);
		dto.setSource(entity.getSource());

		if (entity.getAsset() != null) {
			dto.setAssetId(entity.getAsset().getId());
			dto.setAssetName(entity.getAsset().getName());
		}

		return dto;
	}

	/**
	 * The row's own currency, never the asset's current one: a currency change must not restate the
	 * history. The book target is the last resort of a row built in memory with no asset at all.
	 */
	private Currency currencyOf(Snapshot entity, RateBook book) {
		Currency currency = entity.resolveCurrency();
		return currency != null ? currency : book.target();
	}
}
