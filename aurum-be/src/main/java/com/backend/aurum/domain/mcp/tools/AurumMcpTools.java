package com.backend.aurum.domain.mcp.tools;

import com.backend.aurum.domain.analytics.dto.AnalyticsSummaryDTO;
import com.backend.aurum.domain.analytics.dto.TargetDTO;
import com.backend.aurum.domain.analytics.mapper.TargetMapper;
import com.backend.aurum.domain.analytics.model.Target;
import com.backend.aurum.domain.analytics.service.AnalyticsService;
import com.backend.aurum.domain.analytics.service.TargetService;
import com.backend.aurum.domain.asset.dto.AssetCategoryDTO;
import com.backend.aurum.domain.asset.dto.AssetDTO;
import com.backend.aurum.domain.asset.dto.SnapshotDTO;
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
		assetService.findById(id, user.getId());
		return snapshotService.findByAssetId(id).stream().map(snapshotMapper::toDto).toList();
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
		BigDecimal currentNetWorth = analyticsService.getSummary(user.getId()).getTotalNetWorth();
		return targetService
			.findAll(user.getId())
			.stream()
			.map(t -> targetMapper.toDto(t, currentNetWorth))
			.toList();
	}

	@Tool(description = "Create a new financial asset")
	public AssetDTO addAsset(AssetDTO dto) {
		User user = currentUser();
		Asset asset = assetMapper.toEntity(dto, user.getId());
		Asset saved = assetService.save(asset, dto.getInitialValue(), dto.getReferenceDate());
		return assetMapper.toDto(saved);
	}

	@Tool(description = "Add a value snapshot to an existing asset")
	public SnapshotDTO addSnapshot(SnapshotDTO dto) {
		User user = currentUser();
		Asset asset = assetService.findById(dto.getAssetId(), user.getId());
		BigDecimal exchangeRate = resolveExchangeRate(asset, user, dto);
		return snapshotMapper.toDto(
			snapshotService.saveOrUpdate(
				asset,
				dto.getAmountOriginalCurrency(),
				dto.getReferenceDate(),
				exchangeRate
			)
		);
	}

	@Tool(description = "Create a new financial target/goal")
	public TargetDTO addTarget(TargetDTO dto) {
		User user = currentUser();
		dto.setIsCompleted(false);
		Target target = targetMapper.toEntity(dto, user.getId());
		Target saved = targetService.save(target);
		BigDecimal netWorth = analyticsService.getSummary(user.getId()).getTotalNetWorth();
		return targetMapper.toDto(saved, netWorth);
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
	public AssetCategoryDTO addCategory(AssetCategoryDTO dto) {
		User user = currentUser();
		AssetCategory category = categoryMapper.toEntity(dto, user.getId());
		return categoryMapper.toDto(categoryService.save(category));
	}

	private BigDecimal resolveExchangeRate(Asset asset, User user, SnapshotDTO dto) {
		if (dto.getExchangeRateToBase() != null) return dto.getExchangeRateToBase();
		String assetCurrency = asset.getOriginalCurrency().getValue();
		String userCurrency = user.getCurrency().getValue();
		return assetCurrency.equals(userCurrency)
			? BigDecimal.ONE
			: exchangeRateService.getRate(assetCurrency, userCurrency, dto.getReferenceDate());
	}

	private User currentUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		return (User) auth.getPrincipal();
	}
}
