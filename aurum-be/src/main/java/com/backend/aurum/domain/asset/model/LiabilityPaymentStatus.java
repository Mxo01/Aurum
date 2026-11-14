package com.backend.aurum.domain.asset.model;

/**
 * Outcome of one automatic liability payment attempt. {@code NOT_DUE} is not a skip: nothing was
 * wrong with the asset, its next period simply has not arrived yet.
 */
public enum LiabilityPaymentStatus {
	PAID,
	SKIPPED,
	NOT_DUE
}
