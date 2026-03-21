package com.backend.aurum.domain.asset.dto;

import com.backend.aurum.domain.asset.model.AssetType;
import java.util.UUID;
import lombok.Data;

@Data
public class AssetCategoryDTO {

	private UUID id;
	private String name;
	private AssetType type;
}
