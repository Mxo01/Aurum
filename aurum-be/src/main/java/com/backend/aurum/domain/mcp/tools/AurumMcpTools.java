package com.backend.aurum.domain.mcp.tools;

import com.backend.aurum.domain.analytics.dto.AnalyticsSummaryDTO;
import com.backend.aurum.domain.analytics.dto.CreateTargetDTO;
import com.backend.aurum.domain.analytics.dto.TargetDTO;
import com.backend.aurum.domain.analytics.dto.UpdateTargetDTO;
import com.backend.aurum.domain.analytics.facade.AnalyticsFacade;
import com.backend.aurum.domain.analytics.facade.TargetFacade;
import com.backend.aurum.domain.asset.dto.AssetCategoryDTO;
import com.backend.aurum.domain.asset.dto.AssetDTO;
import com.backend.aurum.domain.asset.dto.CreateAssetCategoryDTO;
import com.backend.aurum.domain.asset.dto.CreateAssetDTO;
import com.backend.aurum.domain.asset.dto.CreateSnapshotDTO;
import com.backend.aurum.domain.asset.dto.SnapshotDTO;
import com.backend.aurum.domain.asset.dto.UpdateAssetCategoryDTO;
import com.backend.aurum.domain.asset.dto.UpdateAssetDTO;
import com.backend.aurum.domain.asset.facade.AssetCategoryFacade;
import com.backend.aurum.domain.asset.facade.AssetFacade;
import com.backend.aurum.domain.asset.facade.SnapshotFacade;
import com.backend.aurum.domain.cashflow.dto.CashFlowEntryDTO;
import com.backend.aurum.domain.cashflow.dto.CashFlowYearDTO;
import com.backend.aurum.domain.cashflow.dto.UpdateCashFlowEntryDTO;
import com.backend.aurum.domain.cashflow.facade.CashFlowFacade;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.model.UserPrincipal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AurumMcpTools {

	private final AssetFacade assetFacade;
	private final SnapshotFacade snapshotFacade;
	private final TargetFacade targetFacade;
	private final AnalyticsFacade analyticsFacade;
	private final AssetCategoryFacade categoryFacade;
	private final CashFlowFacade cashFlowFacade;

	@Value("${mcp.max-batch-size:500}")
	private int maxBatchSize = 500;

	@McpTool(description = "Get all assets for the current user")
	public List<AssetDTO> getAssets() {
		return assetFacade.getAllAssets(currentUser().getId());
	}

	@McpTool(description = "Get all value snapshots for a specific asset")
	public List<SnapshotDTO> getSnapshots(
		@McpToolParam(description = "UUID of the asset") String assetId
	) {
		return snapshotFacade.getSnapshots(java.util.UUID.fromString(assetId), currentUser().getId());
	}

	@McpTool(
		description = "Get financial KPIs: net worth, gross assets, liabilities, allocations, variations"
	)
	public AnalyticsSummaryDTO getKpis() {
		return analyticsFacade.getSummary(currentUser().getId());
	}

	@McpTool(description = "Get all financial targets/goals for the current user")
	public List<TargetDTO> getTargets() {
		return targetFacade.getAllTargets(currentUser().getId());
	}

	@McpTool(description = "Create a new financial asset")
	public AssetDTO addAsset(CreateAssetDTO dto) {
		return assetFacade.createAsset(dto, currentUser().getId());
	}

	@McpTool(
		description = "Create multiple financial assets at once. Use this when importing data in bulk (e.g. from a spreadsheet) to avoid making many separate addAsset calls."
	)
	public List<AssetDTO> bulkAddAssets(
		@McpToolParam(description = "List of assets to create") List<CreateAssetDTO> assets
	) {
		validateBatch(assets, "bulkAddAssets");
		return assets
			.stream()
			.map(dto -> assetFacade.createAsset(dto, currentUser().getId()))
			.toList();
	}

	@McpTool(description = "Update an existing financial asset")
	public AssetDTO updateAsset(UpdateAssetDTO dto) {
		return assetFacade.updateAsset(dto.getId(), dto, currentUser().getId());
	}

	@McpTool(
		description = "Update multiple existing financial assets at once. Use this when updating data in bulk (e.g. from a spreadsheet) to avoid making many separate updateAsset calls."
	)
	public List<AssetDTO> bulkUpdateAssets(
		@McpToolParam(description = "List of assets to update, each must include the asset id") List<
			UpdateAssetDTO
		> assets
	) {
		validateBatch(assets, "bulkUpdateAssets");
		return assets
			.stream()
			.map(dto -> assetFacade.updateAsset(dto.getId(), dto, currentUser().getId()))
			.toList();
	}

	@McpTool(description = "Add a value snapshot to an existing asset")
	public SnapshotDTO addSnapshot(CreateSnapshotDTO dto) {
		return snapshotFacade.createSnapshot(dto, currentUser().getId());
	}

	@McpTool(
		description = "Add multiple value snapshots at once. Use this when importing historical data in bulk (e.g. from a spreadsheet) to avoid making many separate addSnapshot calls."
	)
	public List<SnapshotDTO> bulkAddSnapshots(
		@McpToolParam(description = "List of snapshots to create") List<CreateSnapshotDTO> snapshots
	) {
		validateBatch(snapshots, "bulkAddSnapshots");
		return snapshots
			.stream()
			.map(dto -> snapshotFacade.createSnapshot(dto, currentUser().getId()))
			.toList();
	}

	@McpTool(
		description = "Update an existing snapshot (upserts by asset + month: updates if one exists for that month, creates otherwise)"
	)
	public SnapshotDTO updateSnapshot(CreateSnapshotDTO dto) {
		return snapshotFacade.createSnapshot(dto, currentUser().getId());
	}

	@McpTool(
		description = "Update multiple existing snapshots at once (upserts by asset + month). Use this when updating historical data in bulk to avoid making many separate updateSnapshot calls."
	)
	public List<SnapshotDTO> bulkUpdateSnapshots(
		@McpToolParam(description = "List of snapshots to update") List<CreateSnapshotDTO> snapshots
	) {
		validateBatch(snapshots, "bulkUpdateSnapshots");
		return snapshots
			.stream()
			.map(dto -> snapshotFacade.createSnapshot(dto, currentUser().getId()))
			.toList();
	}

	@McpTool(description = "Create a new financial target/goal")
	public TargetDTO addTarget(CreateTargetDTO dto) {
		return targetFacade.createTarget(dto, currentUser().getId());
	}

	@McpTool(
		description = "Create multiple financial targets/goals at once. Use this when importing data in bulk (e.g. from a spreadsheet) to avoid making many separate addTarget calls."
	)
	public List<TargetDTO> bulkAddTargets(
		@McpToolParam(description = "List of targets to create") List<CreateTargetDTO> targets
	) {
		validateBatch(targets, "bulkAddTargets");
		return targetFacade.bulkCreateTargets(targets, currentUser().getId());
	}

	@McpTool(description = "Update an existing financial target/goal")
	public TargetDTO updateTarget(UpdateTargetDTO dto) {
		return targetFacade.updateTarget(dto.getId(), dto, currentUser().getId());
	}

	@McpTool(
		description = "Update multiple existing financial targets/goals at once. Use this when updating data in bulk to avoid making many separate updateTarget calls."
	)
	public List<TargetDTO> bulkUpdateTargets(
		@McpToolParam(description = "List of targets to update, each must include the target id") List<
			UpdateTargetDTO
		> targets
	) {
		validateBatch(targets, "bulkUpdateTargets");
		return targetFacade.bulkUpdateTargets(targets, currentUser().getId());
	}

	@McpTool(description = "Get all asset categories (system-wide and user-defined)")
	public List<AssetCategoryDTO> getCategories() {
		return categoryFacade.getAllCategories(currentUser().getId());
	}

	@McpTool(description = "Create a new user-defined asset category")
	public AssetCategoryDTO addCategory(CreateAssetCategoryDTO dto) {
		return categoryFacade.createCategory(dto, currentUser().getId());
	}

	@McpTool(
		description = "Create multiple user-defined asset categories at once. Use this when importing data in bulk (e.g. from a spreadsheet) to avoid making many separate addCategory calls."
	)
	public List<AssetCategoryDTO> bulkAddCategories(
		@McpToolParam(description = "List of categories to create") List<
			CreateAssetCategoryDTO
		> categories
	) {
		validateBatch(categories, "bulkAddCategories");
		return categories
			.stream()
			.map(dto -> categoryFacade.createCategory(dto, currentUser().getId()))
			.toList();
	}

	@McpTool(description = "Update an existing user-defined asset category")
	public AssetCategoryDTO updateCategory(UpdateAssetCategoryDTO dto) {
		return categoryFacade.updateCategory(dto.getId(), dto, currentUser().getId());
	}

	@McpTool(
		description = "Update multiple existing user-defined asset categories at once. Use this when updating data in bulk to avoid making many separate updateCategory calls."
	)
	public List<AssetCategoryDTO> bulkUpdateCategories(
		@McpToolParam(
			description = "List of categories to update, each must include the category id"
		) List<UpdateAssetCategoryDTO> categories
	) {
		validateBatch(categories, "bulkUpdateCategories");
		return categories
			.stream()
			.map(dto -> categoryFacade.updateCategory(dto.getId(), dto, currentUser().getId()))
			.toList();
	}

	@McpTool(
		description = "Get monthly cash flow data (earned and spent) for a specific year, including previous year comparison and available years"
	)
	public CashFlowYearDTO getCashFlowYear(
		@McpToolParam(description = "The year to retrieve cash flow data for (e.g. 2025)") Integer year
	) {
		return cashFlowFacade.getYear(currentUser().getId(), year);
	}

	@McpTool(
		description = "Update the earned and/or spent amount for a specific month in a given year. Creates the entry if it does not exist yet."
	)
	public CashFlowEntryDTO updateCashFlowEntry(
		@McpToolParam(description = "The year of the entry (e.g. 2025)") Integer year,
		@McpToolParam(
			description = "The month of the entry (1 = January, 12 = December)"
		) Integer month,
		@McpToolParam(
			description = "The DTO containing the earned and/or spent amounts to set"
		) UpdateCashFlowEntryDTO dto
	) {
		return cashFlowFacade.updateEntry(currentUser().getId(), year, month, dto);
	}

	@McpTool(
		description = "Update multiple monthly cash flow entries at once. Use this when importing or updating data in bulk (e.g. from a spreadsheet) to avoid making many separate updateCashFlowEntry calls."
	)
	public List<CashFlowEntryDTO> bulkUpdateCashFlowEntries(
		@McpToolParam(description = "Year all entries belong to (e.g. 2025)") Integer year,
		@McpToolParam(
			description = "List of monthly entries to update; each item must have a month (1–12), earned, and spent"
		) List<CashFlowEntryDTO> entries
	) {
		validateBatch(entries, "bulkUpdateCashFlowEntries");
		return entries
			.stream()
			.map(entry -> {
				UpdateCashFlowEntryDTO dto = new UpdateCashFlowEntryDTO();
				dto.setEarned(entry.getEarned());
				dto.setSpent(entry.getSpent());
				return cashFlowFacade.updateEntry(currentUser().getId(), year, entry.getMonth(), dto);
			})
			.toList();
	}

	private void validateBatch(List<?> items, String toolName) {
		if (maxBatchSize < 1) {
			throw new IllegalStateException("mcp.max-batch-size must be positive");
		}
		if (items == null) {
			throw new IllegalArgumentException(toolName + " requires a list of items");
		}
		if (items.size() > maxBatchSize) {
			throw new IllegalArgumentException(
				toolName + " accepts at most " + maxBatchSize + " items per call"
			);
		}
	}

	private User currentUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
		return principal.user();
	}
}
