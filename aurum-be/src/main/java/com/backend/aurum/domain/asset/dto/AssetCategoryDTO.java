package com.backend.aurum.domain.asset.dto;

import com.backend.aurum.domain.asset.model.AssetType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.Data;

@Data
@Schema(requiredProperties = { "id", "name", "type", "icon", "isDefault" })
public class AssetCategoryDTO {

	private UUID id;
	private String name;
	private AssetType type;

	@Schema(nullable = true)
	private String icon;

	@JsonProperty("isDefault")
	private boolean isDefault;

	@JsonProperty("isDefault")
	public boolean isDefault() {
		return isDefault;
	}
}
