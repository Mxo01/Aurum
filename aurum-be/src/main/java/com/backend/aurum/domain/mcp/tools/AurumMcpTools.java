package com.backend.aurum.domain.mcp.tools;

import com.backend.aurum.domain.analytics.dto.AnalyticsSummaryDTO;
import com.backend.aurum.domain.analytics.dto.CreateTargetDTO;
import com.backend.aurum.domain.analytics.dto.TargetDTO;
import com.backend.aurum.domain.analytics.dto.UpdateTargetDTO;
import com.backend.aurum.domain.analytics.mapper.TargetMapper;
import com.backend.aurum.domain.analytics.model.Target;
import com.backend.aurum.domain.analytics.service.AnalyticsService;
import com.backend.aurum.domain.analytics.service.TargetService;
import com.backend.aurum.domain.asset.dto.AssetCategoryDTO;
import com.backend.aurum.domain.asset.dto.AssetDTO;
import com.backend.aurum.domain.asset.dto.CreateAssetCategoryDTO;
import com.backend.aurum.domain.asset.dto.CreateAssetDTO;
import com.backend.aurum.domain.asset.dto.CreateSnapshotDTO;
import com.backend.aurum.domain.asset.dto.SnapshotDTO;
import com.backend.aurum.domain.asset.dto.UpdateAssetCategoryDTO;
import com.backend.aurum.domain.asset.dto.UpdateAssetDTO;
import com.backend.aurum.domain.asset.mapper.AssetCategoryMapper;
import com.backend.aurum.domain.asset.mapper.AssetMapper;
import com.backend.aurum.domain.asset.mapper.SnapshotMapper;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.AssetCategory;
import com.backend.aurum.domain.asset.service.AssetCategoryService;
import com.backend.aurum.domain.asset.service.AssetService;
import com.backend.aurum.domain.asset.service.SnapshotService;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.infrastructure.exchange.ExchangeRateService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AurumMcpTools {

	private final AssetService assetService;
	private final AssetMapper assetMapper;
	private final SnapshotService snapshotService;
	private final SnapshotMapper snapshotMapper;
	private final TargetService targetService;
	private final TargetMapper targetMapper;
	private final AnalyticsService analyticsService;
	private final ExchangeRateService exchangeRateService;
	private final AssetCategoryService categoryService;
	private final AssetCategoryMapper categoryMapper;

	@Tool(description = "Get all assets for the current user")
	public List<AssetDTO> getAssets() {
		return assetService.findAll(currentUser().getId()).stream().map(assetMapper::toDto).toList();
	}

	@Tool(description = "Get all value snapshots for a specific asset")
	public List<SnapshotDTO> getSnapshots(
		@ToolParam(description = "UUID of the asset") String assetId
	) {
		User user = currentUser();
		UUID id = UUID.fromString(assetId);
		return snapshotService
			.findByAssetId(id, user.getId())
			.stream()
			.map(snapshotMapper::toDto)
			.toList();
	}

	@Tool(
		description = "Get financial KPIs: net worth, gross assets, liabilities, allocations, variations"
	)
	public AnalyticsSummaryDTO getKpis() {
		return analyticsService.getSummary(currentUser().getId());
	}

	@Tool(description = "Get all financial targets/goals for the current user")
	public List<TargetDTO> getTargets() {
		User user = currentUser();
		BigDecimal netWorth = analyticsService.getSummary(user.getId()).getTotalNetWorth();
		return targetService
			.findAll(user.getId(), netWorth)
			.stream()
			.map(t -> targetMapper.toDto(t, netWorth))
			.toList();
	}

	@Tool(description = "Create a new financial asset")
	public AssetDTO addAsset(CreateAssetDTO dto) {
		User user = currentUser();
		Asset asset = assetMapper.toEntity(dto, user.getId());
		Asset saved = assetService.save(asset, dto.getInitialValue(), dto.getReferenceDate());
		return assetMapper.toDto(saved);
	}

	@Tool(
		description = "Create multiple financial assets at once. Use this when importing data in bulk (e.g. from a spreadsheet) to avoid making many separate addAsset calls."
	)
	public List<AssetDTO> bulkAddAssets(
		@ToolParam(description = "List of assets to create") List<CreateAssetDTO> assets
	) {
		User user = currentUser();
		return assets
			.stream()
			.map(dto -> {
				Asset asset = assetMapper.toEntity(dto, user.getId());
				Asset saved = assetService.save(asset, dto.getInitialValue(), dto.getReferenceDate());
				return assetMapper.toDto(saved);
			})
			.toList();
	}

	@Tool(description = "Update an existing financial asset")
	public AssetDTO updateAsset(UpdateAssetDTO dto) {
		User user = currentUser();
		Asset assetDetails = assetMapper.toEntity(dto, user.getId());
		Asset updated = assetService.update(dto.getId(), assetDetails, user.getId());
		return assetMapper.toDto(updated);
	}

	@Tool(
		description = "Update multiple existing financial assets at once. Use this when updating data in bulk (e.g. from a spreadsheet) to avoid making many separate updateAsset calls."
	)
	public List<AssetDTO> bulkUpdateAssets(
		@ToolParam(description = "List of assets to update, each must include the asset id") List<
			UpdateAssetDTO
		> assets
	) {
		User user = currentUser();
		return assets
			.stream()
			.map(dto -> {
				Asset assetDetails = assetMapper.toEntity(dto, user.getId());
				Asset updated = assetService.update(dto.getId(), assetDetails, user.getId());
				return assetMapper.toDto(updated);
			})
			.toList();
	}

	@Tool(description = "Add a value snapshot to an existing asset")
	public SnapshotDTO addSnapshot(CreateSnapshotDTO dto) {
		User user = currentUser();
		Asset asset = assetService.findById(dto.getAssetId(), user.getId());
		BigDecimal exchangeRate = resolveExchangeRate(
			asset,
			user,
			dto.getExchangeRateToBase(),
			dto.getReferenceDate()
		);
		return snapshotMapper.toDto(
			snapshotService.saveOrUpdate(
				asset,
				dto.getAmountOriginalCurrency(),
				dto.getReferenceDate(),
				exchangeRate
			)
		);
	}

	@Tool(
		description = "Add multiple value snapshots at once. Use this when importing historical data in bulk (e.g. from a spreadsheet) to avoid making many separate addSnapshot calls."
	)
	public List<SnapshotDTO> bulkAddSnapshots(
		@ToolParam(description = "List of snapshots to create") List<CreateSnapshotDTO> snapshots
	) {
		User user = currentUser();
		return snapshots
			.stream()
			.map(dto -> {
				Asset asset = assetService.findById(dto.getAssetId(), user.getId());
				BigDecimal exchangeRate = resolveExchangeRate(
					asset,
					user,
					dto.getExchangeRateToBase(),
					dto.getReferenceDate()
				);
				return snapshotMapper.toDto(
					snapshotService.saveOrUpdate(
						asset,
						dto.getAmountOriginalCurrency(),
						dto.getReferenceDate(),
						exchangeRate
					)
				);
			})
			.toList();
	}

	@Tool(
		description = "Update an existing snapshot (upserts by asset + month: updates if one exists for that month, creates otherwise)"
	)
	public SnapshotDTO updateSnapshot(CreateSnapshotDTO dto) {
		User user = currentUser();
		Asset asset = assetService.findById(dto.getAssetId(), user.getId());
		BigDecimal exchangeRate = resolveExchangeRate(
			asset,
			user,
			dto.getExchangeRateToBase(),
			dto.getReferenceDate()
		);
		return snapshotMapper.toDto(
			snapshotService.saveOrUpdate(
				asset,
				dto.getAmountOriginalCurrency(),
				dto.getReferenceDate(),
				exchangeRate
			)
		);
	}

	@Tool(
		description = "Update multiple existing snapshots at once (upserts by asset + month). Use this when updating historical data in bulk to avoid making many separate updateSnapshot calls."
	)
	public List<SnapshotDTO> bulkUpdateSnapshots(
		@ToolParam(description = "List of snapshots to update") List<CreateSnapshotDTO> snapshots
	) {
		User user = currentUser();
		return snapshots
			.stream()
			.map(dto -> {
				Asset asset = assetService.findById(dto.getAssetId(), user.getId());
				BigDecimal exchangeRate = resolveExchangeRate(
					asset,
					user,
					dto.getExchangeRateToBase(),
					dto.getReferenceDate()
				);
				return snapshotMapper.toDto(
					snapshotService.saveOrUpdate(
						asset,
						dto.getAmountOriginalCurrency(),
						dto.getReferenceDate(),
						exchangeRate
					)
				);
			})
			.toList();
	}

	@Tool(description = "Create a new financial target/goal")
	public TargetDTO addTarget(CreateTargetDTO dto) {
		User user = currentUser();
		Target target = targetMapper.toEntity(dto, user.getId());
		BigDecimal netWorth = analyticsService.getSummary(user.getId()).getTotalNetWorth();
		Target saved = targetService.save(target, netWorth);
		return targetMapper.toDto(saved, netWorth);
	}

	@Tool(
		description = "Create multiple financial targets/goals at once. Use this when importing data in bulk (e.g. from a spreadsheet) to avoid making many separate addTarget calls."
	)
	public List<TargetDTO> bulkAddTargets(
		@ToolParam(description = "List of targets to create") List<CreateTargetDTO> targets
	) {
		User user = currentUser();
		BigDecimal netWorth = analyticsService.getSummary(user.getId()).getTotalNetWorth();
		return targets
			.stream()
			.map(dto -> {
				Target target = targetMapper.toEntity(dto, user.getId());
				Target saved = targetService.save(target, netWorth);
				return targetMapper.toDto(saved, netWorth);
			})
			.toList();
	}

	@Tool(description = "Update an existing financial target/goal")
	public TargetDTO updateTarget(UpdateTargetDTO dto) {
		User user = currentUser();
		Target targetDetails = targetMapper.toEntity(dto, user.getId());
		BigDecimal netWorth = analyticsService.getSummary(user.getId()).getTotalNetWorth();
		Target updated = targetService.update(dto.getId(), targetDetails, netWorth);
		return targetMapper.toDto(updated, netWorth);
	}

	@Tool(
		description = "Update multiple existing financial targets/goals at once. Use this when updating data in bulk to avoid making many separate updateTarget calls."
	)
	public List<TargetDTO> bulkUpdateTargets(
		@ToolParam(description = "List of targets to update, each must include the target id") List<
			UpdateTargetDTO
		> targets
	) {
		User user = currentUser();
		BigDecimal netWorth = analyticsService.getSummary(user.getId()).getTotalNetWorth();
		return targets
			.stream()
			.map(dto -> {
				Target targetDetails = targetMapper.toEntity(dto, user.getId());
				Target updated = targetService.update(dto.getId(), targetDetails, netWorth);
				return targetMapper.toDto(updated, netWorth);
			})
			.toList();
	}

	@Tool(description = "Get all asset categories (system-wide and user-defined)")
	public List<AssetCategoryDTO> getCategories() {
		return categoryService
			.findAll(currentUser().getId())
			.stream()
			.map(categoryMapper::toDto)
			.toList();
	}

	@Tool(description = "Create a new user-defined asset category")
	public AssetCategoryDTO addCategory(CreateAssetCategoryDTO dto) {
		User user = currentUser();
		AssetCategory category = categoryMapper.toEntity(dto, user.getId());
		return categoryMapper.toDto(categoryService.save(category));
	}

	@Tool(
		description = "Create multiple user-defined asset categories at once. Use this when importing data in bulk (e.g. from a spreadsheet) to avoid making many separate addCategory calls."
	)
	public List<AssetCategoryDTO> bulkAddCategories(
		@ToolParam(description = "List of categories to create") List<CreateAssetCategoryDTO> categories
	) {
		User user = currentUser();
		return categories
			.stream()
			.map(dto -> {
				AssetCategory category = categoryMapper.toEntity(dto, user.getId());
				return categoryMapper.toDto(categoryService.save(category));
			})
			.toList();
	}

	@Tool(description = "Update an existing user-defined asset category")
	public AssetCategoryDTO updateCategory(UpdateAssetCategoryDTO dto) {
		User user = currentUser();
		AssetCategory categoryDetails = categoryMapper.toEntity(dto, user.getId());
		AssetCategory updated = categoryService.update(dto.getId(), categoryDetails, user.getId());
		return categoryMapper.toDto(updated);
	}

	@Tool(
		description = "Update multiple existing user-defined asset categories at once. Use this when updating data in bulk to avoid making many separate updateCategory calls."
	)
	public List<AssetCategoryDTO> bulkUpdateCategories(
		@ToolParam(
			description = "List of categories to update, each must include the category id"
		) List<UpdateAssetCategoryDTO> categories
	) {
		User user = currentUser();
		return categories
			.stream()
			.map(dto -> {
				AssetCategory categoryDetails = categoryMapper.toEntity(dto, user.getId());
				AssetCategory updated = categoryService.update(dto.getId(), categoryDetails, user.getId());
				return categoryMapper.toDto(updated);
			})
			.toList();
	}

	private BigDecimal resolveExchangeRate(
		Asset asset,
		User user,
		BigDecimal explicitRate,
		java.time.LocalDate referenceDate
	) {
		if (explicitRate != null) return explicitRate;
		String assetCurrency = asset.getOriginalCurrency().getValue();
		String userCurrency = user.getCurrency().getValue();
		return assetCurrency.equals(userCurrency)
			? BigDecimal.ONE
			: exchangeRateService.getRate(assetCurrency, userCurrency, referenceDate);
	}

	private User currentUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		return (User) auth.getPrincipal();
	}
}
