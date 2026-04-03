package com.backend.aurum.domain.asset.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.LiabilityType;
import com.backend.aurum.domain.asset.model.PaymentFrequency;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.user.enums.Currency;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.infrastructure.exchange.ExchangeRateService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LiabilitySchedulerServiceTest {

	private static final LocalDate FIXED_TODAY = LocalDate.of(2026, 4, 1);

	@Mock
	private AssetRepository assetRepository;

	@Mock
	private SnapshotService snapshotService;

	@Mock
	private ExchangeRateService exchangeRateService;

	private LiabilitySchedulerService testSubject;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(FIXED_TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
		testSubject = new LiabilitySchedulerService(
			assetRepository,
			snapshotService,
			exchangeRateService,
			clock
		);
	}

	private User buildUser(Currency currency) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setCurrency(currency);
		return user;
	}

	private Asset buildAsset(User user, PaymentFrequency frequency, BigDecimal paymentAmount) {
		Asset asset = new Asset();
		asset.setId(UUID.randomUUID());
		asset.setName("Test Liability");
		asset.setUser(user);
		asset.setLiabilityType(LiabilityType.AUTOMATIC);
		asset.setPaymentFrequency(frequency);
		asset.setPaymentAmount(paymentAmount);

		asset.setOriginalCurrency(user.getCurrency());
		asset.setSnapshots(new ArrayList<>());
		return asset;
	}

	private Snapshot buildSnapshot(Asset asset, BigDecimal amount, LocalDate date) {
		Snapshot snapshot = new Snapshot();
		snapshot.setId(UUID.randomUUID());
		snapshot.setAsset(asset);
		snapshot.setAmountOriginalCurrency(amount);
		snapshot.setExchangeRateToBase(BigDecimal.ONE);
		snapshot.setReferenceDate(date);
		return snapshot;
	}

	@Test
	void processAutomaticLiabilityPayments_createsSnapshot_whenPaymentIsDue() {
		// GIVEN
		User mockUser = buildUser(Currency.EUR);
		Asset mockAsset = buildAsset(mockUser, PaymentFrequency.MONTHLY, new BigDecimal("200"));
		Snapshot mockLatestSnapshot = buildSnapshot(
			mockAsset,
			new BigDecimal("1000"),
			FIXED_TODAY.minusMonths(2)
		);
		mockAsset.getSnapshots().add(mockLatestSnapshot);
		when(assetRepository.findAllByLiabilityTypeWithSnapshots(LiabilityType.AUTOMATIC)).thenReturn(
			List.of(mockAsset)
		);

		// WHEN
		testSubject.processAutomaticLiabilityPayments();

		// THEN
		verify(snapshotService).saveOrUpdate(
			eq(mockAsset),
			eq(new BigDecimal("800")),
			eq(FIXED_TODAY),
			eq(BigDecimal.ONE)
		);
	}

	@Test
	void processAutomaticLiabilityPayments_skipsAsset_whenPaymentNotYetDue() {
		// GIVEN
		User mockUser = buildUser(Currency.EUR);
		Asset mockAsset = buildAsset(mockUser, PaymentFrequency.MONTHLY, new BigDecimal("200"));
		Snapshot mockLatestSnapshot = buildSnapshot(
			mockAsset,
			new BigDecimal("1000"),
			FIXED_TODAY.minusDays(5)
		);
		mockAsset.getSnapshots().add(mockLatestSnapshot);
		when(assetRepository.findAllByLiabilityTypeWithSnapshots(LiabilityType.AUTOMATIC)).thenReturn(
			List.of(mockAsset)
		);

		// WHEN
		testSubject.processAutomaticLiabilityPayments();

		// THEN
		verifyNoInteractions(snapshotService);
	}

	@Test
	void processAutomaticLiabilityPayments_skipsAsset_whenNoSnapshots() {
		// GIVEN
		User mockUser = buildUser(Currency.EUR);
		Asset mockAsset = buildAsset(mockUser, PaymentFrequency.MONTHLY, new BigDecimal("200"));
		when(assetRepository.findAllByLiabilityTypeWithSnapshots(LiabilityType.AUTOMATIC)).thenReturn(
			List.of(mockAsset)
		);

		// WHEN
		testSubject.processAutomaticLiabilityPayments();

		// THEN
		verifyNoInteractions(snapshotService);
	}

	@Test
	void processAutomaticLiabilityPayments_skipsAsset_whenNoPaymentFrequency() {
		// GIVEN
		User mockUser = buildUser(Currency.EUR);
		Asset mockAsset = buildAsset(mockUser, null, new BigDecimal("200"));
		Snapshot mockLatestSnapshot = buildSnapshot(
			mockAsset,
			new BigDecimal("1000"),
			FIXED_TODAY.minusMonths(2)
		);
		mockAsset.getSnapshots().add(mockLatestSnapshot);
		when(assetRepository.findAllByLiabilityTypeWithSnapshots(LiabilityType.AUTOMATIC)).thenReturn(
			List.of(mockAsset)
		);

		// WHEN
		testSubject.processAutomaticLiabilityPayments();

		// THEN
		verifyNoInteractions(snapshotService);
	}

	@Test
	void processAutomaticLiabilityPayments_doesNotGoBelowZero() {
		// GIVEN
		User mockUser = buildUser(Currency.EUR);
		Asset mockAsset = buildAsset(mockUser, PaymentFrequency.MONTHLY, new BigDecimal("2000"));
		Snapshot mockLatestSnapshot = buildSnapshot(
			mockAsset,
			new BigDecimal("100"),
			FIXED_TODAY.minusMonths(2)
		);
		mockAsset.getSnapshots().add(mockLatestSnapshot);
		when(assetRepository.findAllByLiabilityTypeWithSnapshots(LiabilityType.AUTOMATIC)).thenReturn(
			List.of(mockAsset)
		);

		// WHEN
		testSubject.processAutomaticLiabilityPayments();

		// THEN — snapshot is saved (value becomes 0, not negative)
		verify(snapshotService).saveOrUpdate(
			eq(mockAsset),
			eq(BigDecimal.ZERO),
			eq(FIXED_TODAY),
			eq(BigDecimal.ONE)
		);
	}

	@Test
	void processAutomaticLiabilityPayments_fetchesExchangeRate_whenCurrenciesDiffer() {
		// GIVEN
		User mockUser = buildUser(Currency.EUR);
		Asset mockAsset = buildAsset(mockUser, PaymentFrequency.MONTHLY, new BigDecimal("200"));
		mockAsset.setOriginalCurrency(Currency.USD);
		Snapshot mockLatestSnapshot = buildSnapshot(
			mockAsset,
			new BigDecimal("1000"),
			FIXED_TODAY.minusMonths(2)
		);
		mockAsset.getSnapshots().add(mockLatestSnapshot);
		when(assetRepository.findAllByLiabilityTypeWithSnapshots(LiabilityType.AUTOMATIC)).thenReturn(
			List.of(mockAsset)
		);
		BigDecimal stubbedRate = new BigDecimal("0.92");
		when(exchangeRateService.getRate("USD", "EUR", FIXED_TODAY)).thenReturn(stubbedRate);

		// WHEN
		testSubject.processAutomaticLiabilityPayments();

		// THEN
		verify(exchangeRateService).getRate("USD", "EUR", FIXED_TODAY);
		verify(snapshotService).saveOrUpdate(
			eq(mockAsset),
			eq(new BigDecimal("800")),
			eq(FIXED_TODAY),
			eq(stubbedRate)
		);
	}

	@Test
	void processAutomaticLiabilityPayments_continuesProcessing_whenOneAssetFails() {
		// GIVEN
		User mockUser = buildUser(Currency.EUR);
		Asset failingAsset = buildAsset(mockUser, PaymentFrequency.MONTHLY, new BigDecimal("200"));
		failingAsset.setOriginalCurrency(Currency.USD);
		Snapshot mockSnapshot = buildSnapshot(
			failingAsset,
			new BigDecimal("1000"),
			FIXED_TODAY.minusMonths(2)
		);
		failingAsset.getSnapshots().add(mockSnapshot);

		Asset successAsset = buildAsset(mockUser, PaymentFrequency.MONTHLY, new BigDecimal("100"));
		Snapshot mockSnapshot2 = buildSnapshot(
			successAsset,
			new BigDecimal("500"),
			FIXED_TODAY.minusMonths(2)
		);
		successAsset.getSnapshots().add(mockSnapshot2);

		when(assetRepository.findAllByLiabilityTypeWithSnapshots(LiabilityType.AUTOMATIC)).thenReturn(
			List.of(failingAsset, successAsset)
		);
		when(exchangeRateService.getRate("USD", "EUR", FIXED_TODAY)).thenThrow(
			new RuntimeException("Rate fetch failed")
		);

		// WHEN
		testSubject.processAutomaticLiabilityPayments();

		// THEN
		verify(snapshotService).saveOrUpdate(
			eq(successAsset),
			eq(new BigDecimal("400")),
			eq(FIXED_TODAY),
			eq(BigDecimal.ONE)
		);
	}

	@Test
	void processAutomaticLiabilityPayments_skipsAsset_whenAlreadyPaidOff() {
		// GIVEN
		User mockUser = buildUser(Currency.EUR);
		Asset mockAsset = buildAsset(mockUser, PaymentFrequency.MONTHLY, new BigDecimal("200"));
		Snapshot mockLatestSnapshot = buildSnapshot(
			mockAsset,
			BigDecimal.ZERO,
			FIXED_TODAY.minusMonths(2)
		);
		mockAsset.getSnapshots().add(mockLatestSnapshot);
		when(assetRepository.findAllByLiabilityTypeWithSnapshots(LiabilityType.AUTOMATIC)).thenReturn(
			List.of(mockAsset)
		);

		// WHEN
		testSubject.processAutomaticLiabilityPayments();

		// THEN
		verifyNoInteractions(snapshotService);
	}
}
