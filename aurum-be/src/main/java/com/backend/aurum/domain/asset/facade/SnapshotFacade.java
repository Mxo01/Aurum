package com.backend.aurum.domain.asset.facade;

import com.backend.aurum.domain.asset.dto.CreateSnapshotDTO;
import com.backend.aurum.domain.asset.dto.SnapshotDTO;
import com.backend.aurum.domain.asset.mapper.SnapshotMapper;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.asset.service.AssetService;
import com.backend.aurum.domain.asset.service.SnapshotService;
import com.backend.aurum.domain.asset.validation.SnapshotValidationService;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.repository.UserRepository;
import com.backend.aurum.infrastructure.exchange.ExchangeRateService;
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
	private final AssetRepository assetRepository;
	private final ExchangeRateService exchangeRateService;
	private final UserRepository userRepository;

	public List<SnapshotDTO> getSnapshots(UUID assetId, UUID userId) {
		return snapshotService.findByAssetId(assetId, userId).stream().map(mapper::toDto).toList();
	}

	public List<SnapshotDTO> getAllSnapshots(UUID userId) {
		return snapshotService.findAll(userId).stream().map(mapper::toDto).toList();
	}

	public SnapshotDTO createSnapshot(CreateSnapshotDTO dto, UUID userId) {
		validationService.validate(dto, userId);

		Asset asset = assetService.findById(dto.getAssetId(), userId);
		User user = userRepository
			.findById(Objects.requireNonNull(userId))
			.orElseThrow(() -> new RuntimeException("User not found"));

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

		if (
			dto.getAmountOriginalCurrency() != null &&
			dto.getAmountOriginalCurrency().compareTo(BigDecimal.ZERO) == 0 &&
			asset.getLiabilityType() != null
		) {
			asset.setIsActive(false);
			assetRepository.save(asset);
		}

		return mapper.toDto(saved);
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
}
