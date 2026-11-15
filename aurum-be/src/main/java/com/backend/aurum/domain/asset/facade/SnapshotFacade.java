package com.backend.aurum.domain.asset.facade;

import com.backend.aurum.domain.asset.dto.CreateSnapshotDTO;
import com.backend.aurum.domain.asset.dto.SnapshotDTO;
import com.backend.aurum.domain.asset.mapper.SnapshotMapper;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.service.AssetService;
import com.backend.aurum.domain.asset.service.SnapshotService;
import com.backend.aurum.domain.asset.validation.SnapshotValidationService;
import com.backend.aurum.domain.shared.currency.CurrencyConversionService;
import com.backend.aurum.domain.shared.currency.RateBook;
import com.backend.aurum.domain.shared.exception.NotFoundException;
import com.backend.aurum.domain.shared.port.ExchangeRateProvider;
import com.backend.aurum.domain.user.enums.Currency;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SnapshotFacade {

	private final SnapshotService snapshotService;
	private final SnapshotMapper mapper;
	private final SnapshotValidationService validationService;
	private final AssetService assetService;
	private final ExchangeRateProvider exchangeRateService;
	private final CurrencyConversionService conversionService;
	private final UserRepository userRepository;

	public List<SnapshotDTO> getSnapshots(UUID assetId, UUID userId) {
		List<Snapshot> snapshots = snapshotService.findByAssetId(assetId, userId);
		RateBook book = bookFor(requireUser(userId).getCurrency(), snapshots);
		return snapshots
			.stream()
			.map(snapshot -> mapper.toDto(snapshot, book))
			.toList();
	}

	public List<SnapshotDTO> getAllSnapshots(UUID userId) {
		List<Snapshot> snapshots = snapshotService.findAll(userId);
		RateBook book = bookFor(requireUser(userId).getCurrency(), snapshots);
		return snapshots
			.stream()
			.map(snapshot -> mapper.toDto(snapshot, book))
			.toList();
	}

	public SnapshotDTO createSnapshot(CreateSnapshotDTO dto, UUID userId) {
		validationService.validate(dto, userId);

		Asset asset = assetService.findById(dto.getAssetId(), userId);
		User user = requireUser(userId);

		BigDecimal exchangeRate = resolveExchangeRate(
			asset,
			user,
			dto.getExchangeRateToBase(),
			dto.getReferenceDate()
		);

		Snapshot saved = snapshotService.saveOrUpdate(
			asset,
			dto.getAmountOriginalCurrency(),
			dto.getReferenceDate(),
			exchangeRate
		);

		RateBook book = bookFor(user.getCurrency(), List.of(saved));

		return mapper.toDto(saved, book);
	}

	public void deleteBulk(UUID assetId, List<UUID> ids, UUID userId) {
		snapshotService.deleteBulk(ids, assetId, userId);
	}

	private BigDecimal resolveExchangeRate(
		Asset asset,
		User user,
		BigDecimal explicitRate,
		LocalDate referenceDate
	) {
		if (explicitRate != null) return explicitRate;
		String assetCurrency = asset.getOriginalCurrency().getValue();
		String userCurrency = user.getCurrency().getValue();
		return assetCurrency.equals(userCurrency)
			? BigDecimal.ONE
			: exchangeRateService.getRate(assetCurrency, userCurrency, referenceDate);
	}

	private RateBook bookFor(Currency target, List<Snapshot> snapshots) {
		return conversionService.book(
			target,
			snapshots
				.stream()
				.map(Snapshot::resolveCurrency)
				.filter(Objects::nonNull)
				.distinct()
				.toList(),
			snapshots.stream().map(Snapshot::getReferenceDate).toList()
		);
	}

	private User requireUser(UUID userId) {
		return userRepository
			.findById(Objects.requireNonNull(userId))
			.orElseThrow(() -> new NotFoundException("User not found"));
	}
}
