package com.backend.aurum.domain.asset.dto;

import com.backend.aurum.domain.asset.model.AssetType;
import lombok.Data;

@Data
public class CreateAssetCategoryDTO {

	private String name;
	private AssetType type;
	private String icon;
}
