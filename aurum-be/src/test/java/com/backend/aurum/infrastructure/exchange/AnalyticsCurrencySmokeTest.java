package com.backend.aurum.infrastructure.exchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.analytics.dto.AnalyticsSummaryDTO;
import com.backend.aurum.domain.analytics.service.AnalyticsService;
import com.backend.aurum.domain.asset.dto.AssetDTO;
import com.backend.aurum.domain.asset.mapper.AssetMapper;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.AssetCategory;
import com.backend.aurum.domain.asset.model.AssetType;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.asset.repository.SnapshotRepository;
import com.backend.aurum.domain.shared.currency.CurrencyConversionService;
import com.backend.aurum.domain.shared.currency.RateBook;
import com.backend.aurum.domain.user.enums.Currency;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.repository.UserRepository;
import com.backend.aurum.infrastructure.config.CacheConfig;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.client.RestClient;

@SpringJUnitConfig(AnalyticsCurrencySmokeTest.Config.class)
class AnalyticsCurrencySmokeTest {

	private static final String CACHE_NAME = "exchangeRates";
	private static final Instant FIXED_INSTANT = Instant.parse("2026-11-14T10:00:00Z");
	private static final ZoneId FIXED_ZONE = ZoneOffset.UTC;
	private static final LocalDate TODAY = LocalDate.of(2026, 11, 14);
	private static final LocalDate IMPACT_WINDOW_START = TODAY.minusMonths(1);
	private static final LocalDate START_SNAPSHOT_DATE = IMPACT_WINDOW_START.minusDays(1);
	private static final LocalDate SNAPSHOT_DATE = TODAY.minusDays(10);
	private static final BigDecimal SNAPSHOT_AMOUNT = new BigDecimal("1000");
	private static final BigDecimal OLD_RATE = new BigDecimal("1.2");
	private static final BigDecimal CURRENT_RATE = new BigDecimal("1.25");
	private static final BigDecimal EXPECTED_TOTAL = new BigDecimal("1250.0000");
	private static final BigDecimal EXPECTED_ALLOCATION = new BigDecimal("100.0000");
	private static final BigDecimal EXPECTED_CURRENCY_IMPACT = new BigDecimal("50");
	private static final String ASSET_NAME = "Etf";
	private static final String CATEGORY_NAME = "Investments";
	private static final String RATES_FIELD = "rates";
	private static final String OLDEST_QUOTE_DATE = "2025-11-03";
	private static final String OLD_QUOTE_DATE = "2026-05-03";
	private static final String MID_QUOTE_DATE = "2026-10-03";
	private static final String IMPACT_START_QUOTE_DATE = "2026-10-13";
	private static final String CURRENT_QUOTE_DATE = "2026-11-13";
	private static final Map<String, Object> STUBBED_RESPONSE = Map.of(
		RATES_FIELD,
		Map.of(
			OLDEST_QUOTE_DATE,
			Map.of(Currency.USD.getValue(), OLD_RATE),
			OLD_QUOTE_DATE,
			Map.of(Currency.USD.getValue(), OLD_RATE),
			MID_QUOTE_DATE,
			Map.of(Currency.USD.getValue(), OLD_RATE),
			IMPACT_START_QUOTE_DATE,
			Map.of(Currency.USD.getValue(), OLD_RATE),
			CURRENT_QUOTE_DATE,
			Map.of(Currency.USD.getValue(), CURRENT_RATE)
		)
	);

	@Autowired
	private AnalyticsService testSubject;

	@Autowired
	private Config config;

	@Autowired
	private CacheManager cacheManager;

	private UUID mockUserId;
	private Asset mockAsset;
	private Snapshot mockStartSnapshot;
	private Snapshot mockSnapshot;
	private AssetDTO mockAssetDto;

	@BeforeEach
	void setUp() {
		mockUserId = UUID.randomUUID();
		mockAsset = buildMockAsset();
		mockStartSnapshot = buildMockSnapshot(START_SNAPSHOT_DATE);
		mockSnapshot = buildMockSnapshot(SNAPSHOT_DATE);
		mockAssetDto = mock(AssetDTO.class);

		when(config.mockClock.instant()).thenReturn(FIXED_INSTANT);
		when(config.mockClock.getZone()).thenReturn(FIXED_ZONE);
		when(config.mockUserRepository.findById(mockUserId)).thenReturn(Optional.of(buildMockUser()));
		when(config.mockAssetRepository.findByUserIdOrderByCreatedAtDesc(mockUserId)).thenReturn(
			List.of(mockAsset)
		);
		when(
			config.mockSnapshotRepository.findByAssetUserIdAndReferenceDateGreaterThanEqual(
				mockUserId,
				TODAY.minusYears(1).minusMonths(2)
			)
		).thenReturn(List.of(mockStartSnapshot, mockSnapshot));
		when(
			config.mockAssetMapper.toDtoLight(
				eq(mockAsset),
				eq(List.of(mockSnapshot, mockStartSnapshot)),
				any(RateBook.class),
				eq(TODAY)
			)
		).thenReturn(mockAssetDto);
		doReturn(config.mockUriSpec).when(config.mockRestClient).get();
		doReturn(config.mockHeadersSpec).when(config.mockUriSpec).uri(anyString(), any(Object[].class));
		doReturn(config.mockResponseSpec).when(config.mockHeadersSpec).retrieve();
		doAnswer(invocation -> {
			config.providerCalls.incrementAndGet();
			return STUBBED_RESPONSE;
		})
			.when(config.mockResponseSpec)
			.body(any(ParameterizedTypeReference.class));
		config.providerCalls.set(0);
		cacheManager.getCache(CACHE_NAME).clear();
	}

	@Test
	void getSummary_convertsEveryTotalIntoTheUserCurrency_whenRatesComeFromTheCachedProvider() {
		// WHEN
		AnalyticsSummaryDTO expectedSummary = testSubject.getSummary(mockUserId);

		// THEN
		assertThat(expectedSummary.getTotalNetWorth()).isEqualByComparingTo(EXPECTED_TOTAL);
		assertThat(expectedSummary.getTotalGrossAssets()).isEqualByComparingTo(EXPECTED_TOTAL);
		assertThat(expectedSummary.getTotalLiabilities()).isZero();
		assertThat(expectedSummary.getCurrencyImpact()).isEqualByComparingTo(EXPECTED_CURRENCY_IMPACT);
		assertThat(expectedSummary.getAssetAllocation()).containsEntry(
			CATEGORY_NAME,
			EXPECTED_ALLOCATION
		);
		assertThat(expectedSummary.getTopAssets()).containsExactly(mockAssetDto);
		assertThat(config.providerCalls).hasValue(1);
	}

	@Test
	void getSummary_reusesTheCachedRates_whenTheSameAggregateIsRequestedAgain() {
		// WHEN
		AnalyticsSummaryDTO expectedSummary = testSubject.getSummary(mockUserId);
		AnalyticsSummaryDTO expectedRepeatedSummary = testSubject.getSummary(mockUserId);

		// THEN
		assertThat(expectedRepeatedSummary).isEqualTo(expectedSummary);
		assertThat(config.providerCalls).hasValue(1);
	}

	private Asset buildMockAsset() {
		AssetCategory mockCategory = new AssetCategory();
		mockCategory.setName(CATEGORY_NAME);
		mockCategory.setType(AssetType.ASSET);
		Asset asset = new Asset();
		asset.setId(UUID.randomUUID());
		asset.setName(ASSET_NAME);
		asset.setCategory(mockCategory);
		asset.setOriginalCurrency(Currency.GBP);
		return asset;
	}

	private Snapshot buildMockSnapshot(LocalDate referenceDate) {
		Snapshot snapshot = new Snapshot();
		snapshot.setId(UUID.randomUUID());
		snapshot.setAsset(mockAsset);
		snapshot.setReferenceDate(referenceDate);
		snapshot.setAmountOriginalCurrency(SNAPSHOT_AMOUNT);
		snapshot.setCurrency(Currency.EUR);
		return snapshot;
	}

	private User buildMockUser() {
		User user = new User();
		user.setId(mockUserId);
		user.setCurrency(Currency.USD);
		return user;
	}

	@Configuration
	@Import(
		{
			CacheConfig.class,
			ExchangeRateCacheKey.class,
			ExchangeRateService.class,
			CurrencyConversionService.class,
			AnalyticsService.class
		}
	)
	static class Config {

		final RestClient mockRestClient = mock();
		final RestClient.RequestHeadersUriSpec<?> mockUriSpec = mock();
		final RestClient.RequestHeadersSpec<?> mockHeadersSpec = mock();
		final RestClient.ResponseSpec mockResponseSpec = mock();
		final SnapshotRepository mockSnapshotRepository = mock();
		final AssetRepository mockAssetRepository = mock();
		final UserRepository mockUserRepository = mock();
		final AssetMapper mockAssetMapper = mock();
		final Clock mockClock = mock();
		final AtomicInteger providerCalls = new AtomicInteger();

		@Bean
		RestClient mockRestClient() {
			return mockRestClient;
		}

		@Bean
		SnapshotRepository snapshotRepository() {
			return mockSnapshotRepository;
		}

		@Bean
		AssetRepository assetRepository() {
			return mockAssetRepository;
		}

		@Bean
		UserRepository userRepository() {
			return mockUserRepository;
		}

		@Bean
		AssetMapper assetMapper() {
			return mockAssetMapper;
		}

		@Bean
		Clock clock() {
			return mockClock;
		}
	}
}
