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
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.model.UserPrincipal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
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

	@Tool(description = "Get all assets for the current user")
	public List<AssetDTO> getAssets() {
		return assetFacade.getAllAssets(currentUser().getId());
	}

	@Tool(description = "Get all value snapshots for a specific asset")
	public List<SnapshotDTO> getSnapshots(
		@ToolParam(description = "UUID of the asset") String assetId
	) {
		return snapshotFacade.getSnapshots(java.util.UUID.fromString(assetId), currentUser().getId());
	}

	@Tool(
		description = "Get financial KPIs: net worth, gross assets, liabilities, allocations, variations"
	)
	public AnalyticsSummaryDTO getKpis() {
		return analyticsFacade.getSummary(currentUser().getId());
	}

	@Tool(description = "Get all financial targets/goals for the current user")
	public List<TargetDTO> getTargets() {
		return targetFacade.getAllTargets(currentUser().getId());
	}

	@Tool(description = "Create a new financial asset")
	public AssetDTO addAsset(CreateAssetDTO dto) {
		return assetFacade.createAsset(dto, currentUser().getId());
	}

	@Tool(
		description = "Create multiple financial assets at once. Use this when importing data in bulk (e.g. from a spreadsheet) to avoid making many separate addAsset calls."
	)
	public List<AssetDTO> bulkAddAssets(
		@ToolParam(description = "List of assets to create") List<CreateAssetDTO> assets
	) {
		return assets
			.stream()
			.map(dto -> assetFacade.createAsset(dto, currentUser().getId()))
			.toList();
	}

	@Tool(description = "Update an existing financial asset")
	public AssetDTO updateAsset(UpdateAssetDTO dto) {
		return assetFacade.updateAsset(dto.getId(), dto, currentUser().getId());
	}

	@Tool(
		description = "Update multiple existing financial assets at once. Use this when updating data in bulk (e.g. from a spreadsheet) to avoid making many separate updateAsset calls."
	)
	public List<AssetDTO> bulkUpdateAssets(
		@ToolParam(description = "List of assets to update, each must include the asset id") List<
			UpdateAssetDTO
		> assets
	) {
		return assets
			.stream()
			.map(dto -> assetFacade.updateAsset(dto.getId(), dto, currentUser().getId()))
			.toList();
	}

	@Tool(description = "Add a value snapshot to an existing asset")
	public SnapshotDTO addSnapshot(CreateSnapshotDTO dto) {
		return snapshotFacade.createSnapshot(dto, currentUser().getId());
	}

	@Tool(
		description = "Add multiple value snapshots at once. Use this when importing historical data in bulk (e.g. from a spreadsheet) to avoid making many separate addSnapshot calls."
	)
	public List<SnapshotDTO> bulkAddSnapshots(
		@ToolParam(description = "List of snapshots to create") List<CreateSnapshotDTO> snapshots
	) {
		return snapshots
			.stream()
			.map(dto -> snapshotFacade.createSnapshot(dto, currentUser().getId()))
			.toList();
	}

	@Tool(
		description = "Update an existing snapshot (upserts by asset + month: updates if one exists for that month, creates otherwise)"
	)
	public SnapshotDTO updateSnapshot(CreateSnapshotDTO dto) {
		return snapshotFacade.createSnapshot(dto, currentUser().getId());
	}

	@Tool(
		description = "Update multiple existing snapshots at once (upserts by asset + month). Use this when updating historical data in bulk to avoid making many separate updateSnapshot calls."
	)
	public List<SnapshotDTO> bulkUpdateSnapshots(
		@ToolParam(description = "List of snapshots to update") List<CreateSnapshotDTO> snapshots
	) {
		return snapshots
			.stream()
			.map(dto -> snapshotFacade.createSnapshot(dto, currentUser().getId()))
			.toList();
	}

	@Tool(description = "Create a new financial target/goal")
	public TargetDTO addTarget(CreateTargetDTO dto) {
		return targetFacade.createTarget(dto, currentUser().getId());
	}

	@Tool(
		description = "Create multiple financial targets/goals at once. Use this when importing data in bulk (e.g. from a spreadsheet) to avoid making many separate addTarget calls."
	)
	public List<TargetDTO> bulkAddTargets(
		@ToolParam(description = "List of targets to create") List<CreateTargetDTO> targets
	) {
		return targetFacade.bulkCreateTargets(targets, currentUser().getId());
	}

	@Tool(description = "Update an existing financial target/goal")
	public TargetDTO updateTarget(UpdateTargetDTO dto) {
		return targetFacade.updateTarget(dto.getId(), dto, currentUser().getId());
	}

	@Tool(
		description = "Update multiple existing financial targets/goals at once. Use this when updating data in bulk to avoid making many separate updateTarget calls."
	)
	public List<TargetDTO> bulkUpdateTargets(
		@ToolParam(description = "List of targets to update, each must include the target id") List<
			UpdateTargetDTO
		> targets
	) {
		return targetFacade.bulkUpdateTargets(targets, currentUser().getId());
	}

	@Tool(description = "Get all asset categories (system-wide and user-defined)")
	public List<AssetCategoryDTO> getCategories() {
		return categoryFacade.getAllCategories(currentUser().getId());
	}

	@Tool(description = "Create a new user-defined asset category")
	public AssetCategoryDTO addCategory(CreateAssetCategoryDTO dto) {
		return categoryFacade.createCategory(dto, currentUser().getId());
	}

	@Tool(
		description = "Create multiple user-defined asset categories at once. Use this when importing data in bulk (e.g. from a spreadsheet) to avoid making many separate addCategory calls."
	)
	public List<AssetCategoryDTO> bulkAddCategories(
		@ToolParam(description = "List of categories to create") List<CreateAssetCategoryDTO> categories
	) {
		return categories
			.stream()
			.map(dto -> categoryFacade.createCategory(dto, currentUser().getId()))
			.toList();
	}

	@Tool(description = "Update an existing user-defined asset category")
	public AssetCategoryDTO updateCategory(UpdateAssetCategoryDTO dto) {
		return categoryFacade.updateCategory(dto.getId(), dto, currentUser().getId());
	}

	@Tool(
		description = "Update multiple existing user-defined asset categories at once. Use this when updating data in bulk to avoid making many separate updateCategory calls."
	)
	public List<AssetCategoryDTO> bulkUpdateCategories(
		@ToolParam(
			description = "List of categories to update, each must include the category id"
		) List<UpdateAssetCategoryDTO> categories
	) {
		return categories
			.stream()
			.map(dto -> categoryFacade.updateCategory(dto.getId(), dto, currentUser().getId()))
			.toList();
	}

	private User currentUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
		return principal.user();
	}
}
