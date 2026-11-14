package com.backend.aurum.domain.asset.facade;

import com.backend.aurum.domain.asset.dto.AssetDTO;
import com.backend.aurum.domain.asset.dto.CreateAssetDTO;
import com.backend.aurum.domain.asset.dto.UpdateAssetDTO;
import com.backend.aurum.domain.asset.mapper.AssetMapper;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.SnapshotRepository;
import com.backend.aurum.domain.asset.service.AssetService;
import com.backend.aurum.domain.asset.validation.AssetValidationService;
import com.backend.aurum.domain.shared.currency.CurrencyConversionService;
import com.backend.aurum.domain.shared.currency.RateBook;
import com.backend.aurum.domain.shared.exception.NotFoundException;
import com.backend.aurum.domain.user.enums.Currency;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AssetFacade {

	private final AssetService assetService;
	private final AssetMapper mapper;
	private final AssetValidationService validationService;
	private final SnapshotRepository snapshotRepository;
	private final UserRepository userRepository;
	private final CurrencyConversionService conversionService;
	private final Clock clock;

	public List<AssetDTO> getAllAssets(UUID userId) {
		List<Asset> assets = assetService.findAll(userId);
		if (assets.isEmpty()) return List.of();

		Map<UUID, List<Snapshot>> latestTwoByAsset = latestSnapshotsByAsset(userId);
		LocalDate today = LocalDate.now(clock);
		RateBook book = conversionService.book(
			currencyOf(userId),
			assets.stream().map(Asset::getOriginalCurrency).distinct().toList(),
			List.of(today)
		);

		return assets
			.stream()
			.map(a ->
				mapper.toDtoLight(a, latestTwoByAsset.getOrDefault(a.getId(), List.of()), book, today)
			)
			.toList();
	}

	public AssetDTO getAsset(UUID assetId, UUID userId) {
		Asset asset = assetService.findById(assetId, userId);
		return mapper.toDto(asset, bookFor(userId, List.of(asset)));
	}

	public AssetDTO createAsset(CreateAssetDTO dto, UUID userId) {
		validationService.validate(dto);
		Asset asset = mapper.toEntity(dto, userId);
		Asset saved = assetService.save(asset, dto.getInitialValue(), dto.getReferenceDate());
		return mapper.toDto(saved, bookFor(userId, List.of(saved)));
	}

	public AssetDTO updateAsset(UUID id, UpdateAssetDTO dto, UUID userId) {
		validationService.validate(dto);
		Asset assetDetails = mapper.toEntity(dto, userId);
		Asset updated = assetService.update(id, assetDetails, userId);
		return mapper.toDto(updated, bookFor(userId, List.of(updated)));
	}

	public AssetDTO updateAssetLight(UUID id, UpdateAssetDTO dto, UUID userId) {
		validationService.validate(dto);
		Asset assetDetails = mapper.toEntity(dto, userId);
		Asset updated = assetService.update(id, assetDetails, userId);
		List<Snapshot> latestTwo = snapshotRepository.findTop2ByAssetIdOrderByReferenceDateDesc(id);
		LocalDate today = LocalDate.now(clock);
		RateBook book = conversionService.book(
			currencyOf(userId),
			List.of(updated.getOriginalCurrency()),
			List.of(today)
		);
		return mapper.toDtoLight(updated, latestTwo, book, today);
	}

	public void deleteAsset(UUID id, UUID userId) {
		assetService.delete(id, userId);
	}

	private Map<UUID, List<Snapshot>> latestSnapshotsByAsset(UUID userId) {
		return snapshotRepository
			.findByAssetUserIdOrderByReferenceDateDesc(userId)
			.stream()
			.collect(Collectors.groupingBy(s -> s.getAsset().getId()))
			.entrySet()
			.stream()
			.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream().limit(2).toList()));
	}

	private RateBook bookFor(UUID userId, List<Asset> assets) {
		return conversionService.book(
			currencyOf(userId),
			assets.stream().map(Asset::getOriginalCurrency).distinct().toList(),
			assets
				.stream()
				.flatMap(a -> a.getSnapshots() == null ? Stream.empty() : a.getSnapshots().stream())
				.map(Snapshot::getReferenceDate)
				.toList()
		);
	}

	private Currency currencyOf(UUID userId) {
		User user = userRepository
			.findById(Objects.requireNonNull(userId))
			.orElseThrow(() -> new NotFoundException("User not found"));
		return user.getCurrency();
	}
}
