package com.backend.aurum.domain.asset.model;

import com.backend.aurum.domain.user.enums.Currency;
import java.util.Optional;

/**
 * What an update asked to change. A null field means "leave the stored value alone": a rename must
 * not disarm a liability or flip its currency. The liability block is all-or-nothing — present
 * replaces the three columns, empty keeps them.
 */
public record AssetUpdate(
	String name,
	Optional<AssetCategory> category,
	Currency originalCurrency,
	Boolean isFavorite,
	Optional<LiabilityConfiguration> liability
) {}
