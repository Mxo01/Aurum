package com.backend.aurum.domain.asset.model;

import com.backend.aurum.domain.user.enums.Currency;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The production shape behind the liability-classification incident: fifteen assets, every one of
 * them sitting in an {@code ASSET} category, twelve of them still carrying a legacy
 * {@code liability_type = MANUAL} with {@code payment_amount = 0}. The totals are the rehearsal
 * replica's: 1,138.66 held by the three untyped rows and 46,841.00 by the twelve legacy ones.
 *
 * <p>A named fixture rather than literals in a test body: the incident is a shape, and every
 * assertion that guards it has to read the same fifteen rows.
 */
public final class ProductionShapeFixture {

	public static final int ASSET_COUNT = 15;

	public static final int LEGACY_LIABILITY_TYPE_COUNT = 12;

	public static final int UNTYPED_COUNT = 3;

	public static final BigDecimal TOTAL_LATEST_VALUE = new BigDecimal("47979.66");

	public static final BigDecimal LEGACY_LIABILITY_TYPE_LATEST_VALUE = new BigDecimal("46841.00");

	public static final BigDecimal UNTYPED_LATEST_VALUE = new BigDecimal("1138.66");

	public static final LiabilityType LEGACY_LIABILITY_TYPE = LiabilityType.MANUAL;

	public static final BigDecimal LEGACY_PAYMENT_AMOUNT = new BigDecimal("0.0000");

	public static final String CASH_CATEGORY = "Cash";

	public static final String INVESTMENT_CATEGORY = "Investments";

	public static final String PROPERTY_CATEGORY = "Property";

	private static final Currency FIXTURE_CURRENCY = Currency.EUR;

	private static final String ASSET_ID_FORMAT = "00000000-0000-4000-8000-%012d";

	private static final String SNAPSHOT_ID_FORMAT = "00000000-0000-4000-9000-%012d";

	private static final List<String> LEGACY_NAMES = List.of(
		"Savings account",
		"Brokerage",
		"Apartment",
		"Pension fund",
		"Index fund",
		"Bond ladder",
		"Car",
		"Gold",
		"Emergency fund",
		"Art",
		"Checking account",
		"Crypto"
	);

	private static final List<BigDecimal> LEGACY_VALUES = List.of(
		new BigDecimal("12000.00"),
		new BigDecimal("8000.00"),
		new BigDecimal("6500.00"),
		new BigDecimal("5000.00"),
		new BigDecimal("4100.00"),
		new BigDecimal("3900.00"),
		new BigDecimal("2541.00"),
		new BigDecimal("2000.00"),
		new BigDecimal("1500.00"),
		new BigDecimal("900.00"),
		new BigDecimal("250.00"),
		new BigDecimal("150.00")
	);

	private static final List<String> LEGACY_CATEGORIES = List.of(
		CASH_CATEGORY,
		INVESTMENT_CATEGORY,
		PROPERTY_CATEGORY,
		INVESTMENT_CATEGORY,
		INVESTMENT_CATEGORY,
		INVESTMENT_CATEGORY,
		PROPERTY_CATEGORY,
		INVESTMENT_CATEGORY,
		CASH_CATEGORY,
		INVESTMENT_CATEGORY,
		CASH_CATEGORY,
		INVESTMENT_CATEGORY
	);

	private static final List<String> UNTYPED_NAMES = List.of(
		"Cash wallet",
		"Gift account",
		"Deposit"
	);

	private static final List<BigDecimal> UNTYPED_VALUES = List.of(
		new BigDecimal("400.00"),
		new BigDecimal("500.00"),
		new BigDecimal("238.66")
	);

	private static final List<String> UNTYPED_CATEGORIES = List.of(
		CASH_CATEGORY,
		CASH_CATEGORY,
		CASH_CATEGORY
	);

	private ProductionShapeFixture() {}

	public static ProductionShape create() {
		List<Asset> assets = new ArrayList<>();
		Map<UUID, BigDecimal> latestValues = new LinkedHashMap<>();
		for (int index = 0; index < LEGACY_LIABILITY_TYPE_COUNT; index++) {
			Asset asset = buildAsset(
				assetId(assets.size()),
				LEGACY_NAMES.get(index),
				LEGACY_CATEGORIES.get(index),
				LEGACY_LIABILITY_TYPE,
				LEGACY_PAYMENT_AMOUNT
			);
			assets.add(asset);
			latestValues.put(asset.getId(), LEGACY_VALUES.get(index));
		}
		for (int index = 0; index < UNTYPED_COUNT; index++) {
			Asset asset = buildAsset(
				assetId(assets.size()),
				UNTYPED_NAMES.get(index),
				UNTYPED_CATEGORIES.get(index),
				null,
				null
			);
			assets.add(asset);
			latestValues.put(asset.getId(), UNTYPED_VALUES.get(index));
		}
		return new ProductionShape(List.copyOf(assets), Map.copyOf(latestValues));
	}

	private static UUID assetId(int position) {
		return UUID.fromString(ASSET_ID_FORMAT.formatted(position));
	}

	private static UUID snapshotId(int position) {
		return UUID.fromString(SNAPSHOT_ID_FORMAT.formatted(position));
	}

	private static Asset buildAsset(
		UUID id,
		String name,
		String categoryName,
		LiabilityType liabilityType,
		BigDecimal paymentAmount
	) {
		Asset mockAsset = new Asset();
		mockAsset.setId(id);
		mockAsset.setName(name);
		mockAsset.setCategory(buildCategory(categoryName));
		mockAsset.setOriginalCurrency(FIXTURE_CURRENCY);
		mockAsset.setIsFavorite(true);
		mockAsset.setLiabilityType(liabilityType);
		mockAsset.setPaymentAmount(paymentAmount);
		return mockAsset;
	}

	private static AssetCategory buildCategory(String categoryName) {
		AssetCategory mockCategory = new AssetCategory();
		mockCategory.setName(categoryName);
		mockCategory.setType(AssetType.ASSET);
		return mockCategory;
	}

	private static Snapshot buildSnapshot(
		UUID id,
		Asset asset,
		BigDecimal amount,
		LocalDate referenceDate
	) {
		Snapshot mockSnapshot = new Snapshot();
		mockSnapshot.setId(id);
		mockSnapshot.setAsset(asset);
		mockSnapshot.setAmountOriginalCurrency(amount);
		mockSnapshot.setReferenceDate(referenceDate);
		mockSnapshot.setCurrency(FIXTURE_CURRENCY);
		mockSnapshot.setExchangeRateToBase(BigDecimal.ONE);
		return mockSnapshot;
	}

	public record ProductionShape(List<Asset> assets, Map<UUID, BigDecimal> latestValues) {
		public List<Snapshot> snapshotsAt(LocalDate referenceDate) {
			List<Snapshot> snapshots = new ArrayList<>();
			for (int position = 0; position < assets.size(); position++) {
				Asset asset = assets.get(position);
				snapshots.add(
					buildSnapshot(snapshotId(position), asset, latestValues.get(asset.getId()), referenceDate)
				);
			}
			return List.copyOf(snapshots);
		}

		public BigDecimal totalLatestValue() {
			return latestValues.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
		}
	}
}
