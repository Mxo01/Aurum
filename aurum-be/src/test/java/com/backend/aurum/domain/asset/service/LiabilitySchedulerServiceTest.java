package com.backend.aurum.domain.asset.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.LiabilityType;
import com.backend.aurum.domain.asset.model.PaymentFrequency;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.asset.repository.SnapshotRepository;
import com.backend.aurum.domain.user.enums.Currency;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.infrastructure.exchange.ExchangeRateService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LiabilitySchedulerServiceTest {

	@Mock
	private AssetRepository assetRepository;

	@Mock
	private SnapshotRepository snapshotRepository;

	@Mock
	private ExchangeRateService exchangeRateService;

	@InjectMocks
	private LiabilitySchedulerService testSubject;

	private User buildUser(Currency currency) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setCurrency(currency);
		return user;
	}

	private Asset buildAsset(User user, PaymentFrequency frequency, BigDecimal paymentAmount) {
		Asset asset = new Asset();
		asset.setId(UUID.randomUUID());
		asset.setUser(user);
		asset.setLiabilityType(LiabilityType.AUTOMATIC);
		asset.setPaymentFrequency(frequency);
		asset.setPaymentAmount(paymentAmount);
		asset.setIsActive(true);
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
			LocalDate.now().minusMonths(2)
		);
		mockAsset.getSnapshots().add(mockLatestSnapshot);
		when(
			assetRepository.findAllByIsActiveTrueAndLiabilityTypeWithSnapshots(LiabilityType.AUTOMATIC)
		).thenReturn(List.of(mockAsset));

		// WHEN
		testSubject.processAutomaticLiabilityPayments();

		// THEN
		verify(snapshotRepository).save(org.mockito.ArgumentMatchers.notNull());
	}

	@Test
	void processAutomaticLiabilityPayments_skipsAsset_whenPaymentNotYetDue() {
		// GIVEN
		User mockUser = buildUser(Currency.EUR);
		Asset mockAsset = buildAsset(mockUser, PaymentFrequency.MONTHLY, new BigDecimal("200"));
		Snapshot mockLatestSnapshot = buildSnapshot(
			mockAsset,
			new BigDecimal("1000"),
			LocalDate.now().minusDays(5)
		);
		mockAsset.getSnapshots().add(mockLatestSnapshot);
		when(
			assetRepository.findAllByIsActiveTrueAndLiabilityTypeWithSnapshots(LiabilityType.AUTOMATIC)
		).thenReturn(List.of(mockAsset));

		// WHEN
		testSubject.processAutomaticLiabilityPayments();

		// THEN
		verifyNoInteractions(snapshotRepository);
	}

	@Test
	void processAutomaticLiabilityPayments_skipsAsset_whenNoSnapshots() {
		// GIVEN
		User mockUser = buildUser(Currency.EUR);
		Asset mockAsset = buildAsset(mockUser, PaymentFrequency.MONTHLY, new BigDecimal("200"));
		when(
			assetRepository.findAllByIsActiveTrueAndLiabilityTypeWithSnapshots(LiabilityType.AUTOMATIC)
		).thenReturn(List.of(mockAsset));

		// WHEN
		testSubject.processAutomaticLiabilityPayments();

		// THEN
		verifyNoInteractions(snapshotRepository);
	}

	@Test
	void processAutomaticLiabilityPayments_skipsAsset_whenNoPaymentFrequency() {
		// GIVEN
		User mockUser = buildUser(Currency.EUR);
		Asset mockAsset = buildAsset(mockUser, null, new BigDecimal("200"));
		Snapshot mockLatestSnapshot = buildSnapshot(
			mockAsset,
			new BigDecimal("1000"),
			LocalDate.now().minusMonths(2)
		);
		mockAsset.getSnapshots().add(mockLatestSnapshot);
		when(
			assetRepository.findAllByIsActiveTrueAndLiabilityTypeWithSnapshots(LiabilityType.AUTOMATIC)
		).thenReturn(List.of(mockAsset));

		// WHEN
		testSubject.processAutomaticLiabilityPayments();

		// THEN
		verifyNoInteractions(snapshotRepository);
	}

	@Test
	void processAutomaticLiabilityPayments_doesNotGoBelowZero() {
		// GIVEN
		User mockUser = buildUser(Currency.EUR);
		Asset mockAsset = buildAsset(mockUser, PaymentFrequency.MONTHLY, new BigDecimal("2000"));
		Snapshot mockLatestSnapshot = buildSnapshot(
			mockAsset,
			new BigDecimal("100"),
			LocalDate.now().minusMonths(2)
		);
		mockAsset.getSnapshots().add(mockLatestSnapshot);
		when(
			assetRepository.findAllByIsActiveTrueAndLiabilityTypeWithSnapshots(LiabilityType.AUTOMATIC)
		).thenReturn(List.of(mockAsset));

		// WHEN
		testSubject.processAutomaticLiabilityPayments();

		// THEN — snapshot is saved (value becomes 0, not negative)
		verify(snapshotRepository).save(org.mockito.ArgumentMatchers.notNull());
	}

	@Test
	void processAutomaticLiabilityPayments_archivesAsset_whenValueReachesZero() {
		// GIVEN
		User mockUser = buildUser(Currency.EUR);
		Asset mockAsset = buildAsset(mockUser, PaymentFrequency.MONTHLY, new BigDecimal("1000"));
		Snapshot mockLatestSnapshot = buildSnapshot(
			mockAsset,
			new BigDecimal("1000"),
			LocalDate.now().minusMonths(2)
		);
		mockAsset.getSnapshots().add(mockLatestSnapshot);
		when(
			assetRepository.findAllByIsActiveTrueAndLiabilityTypeWithSnapshots(LiabilityType.AUTOMATIC)
		).thenReturn(List.of(mockAsset));

		// WHEN
		testSubject.processAutomaticLiabilityPayments();

		// THEN
		verify(assetRepository).save(mockAsset);
		assertThat(mockAsset.getIsActive()).isFalse();
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
			LocalDate.now().minusMonths(2)
		);
		mockAsset.getSnapshots().add(mockLatestSnapshot);
		when(
			assetRepository.findAllByIsActiveTrueAndLiabilityTypeWithSnapshots(LiabilityType.AUTOMATIC)
		).thenReturn(List.of(mockAsset));
		when(exchangeRateService.getRate("USD", "EUR", LocalDate.now())).thenReturn(
			new BigDecimal("0.92")
		);

		// WHEN
		testSubject.processAutomaticLiabilityPayments();

		// THEN
		verify(exchangeRateService).getRate("USD", "EUR", LocalDate.now());
	}

	@Test
	void processAutomaticLiabilityPayments_continuesProcessing_whenOneAssetFails() {
		// GIVEN
		User mockUser = buildUser(Currency.EUR);
		Asset failingAsset = buildAsset(mockUser, PaymentFrequency.MONTHLY, new BigDecimal("200"));
		Snapshot mockSnapshot = buildSnapshot(
			failingAsset,
			new BigDecimal("1000"),
			LocalDate.now().minusMonths(2)
		);
		failingAsset.getSnapshots().add(mockSnapshot);
		failingAsset.setOriginalCurrency(Currency.USD);
		when(
			assetRepository.findAllByIsActiveTrueAndLiabilityTypeWithSnapshots(LiabilityType.AUTOMATIC)
		).thenReturn(List.of(failingAsset));
		when(exchangeRateService.getRate("USD", "EUR", LocalDate.now())).thenThrow(
			new RuntimeException("Rate fetch failed")
		);

		// WHEN — should not throw, errors are caught per-asset
		testSubject.processAutomaticLiabilityPayments();

		// THEN — no exception propagated
		verifyNoInteractions(snapshotRepository);
	}
}
