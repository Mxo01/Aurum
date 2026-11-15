package com.backend.aurum.domain.asset.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.model.SnapshotSource;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.asset.repository.SnapshotRepository;
import com.backend.aurum.domain.shared.exception.AccessDeniedException;
import com.backend.aurum.domain.shared.exception.NotFoundException;
import com.backend.aurum.domain.user.enums.Currency;
import com.backend.aurum.domain.user.model.User;
import jakarta.persistence.Version;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SnapshotServiceTest {

	private static final LocalDate REFERENCE_DATE = LocalDate.of(2026, 3, 10);
	private static final LocalDate EARLIER_REFERENCE_DATE = LocalDate.of(2026, 3, 3);
	private static final LocalDate LATER_REFERENCE_DATE = LocalDate.of(2026, 3, 20);
	private static final LocalDate NEXT_MONTH_REFERENCE_DATE = LocalDate.of(2026, 4, 2);
	private static final LocalDate SCHEDULED_REFERENCE_DATE = LocalDate.of(2026, 3, 17);
	private static final LocalDate MONTH_START = LocalDate.of(2026, 3, 1);
	private static final LocalDate MONTH_END = LocalDate.of(2026, 3, 31);
	private static final BigDecimal USER_AMOUNT = new BigDecimal("5000");
	private static final BigDecimal LATEST_ROW_AMOUNT = new BigDecimal("5000.0000");
	private static final BigDecimal EARLIER_USER_AMOUNT = new BigDecimal("4000");
	private static final BigDecimal SCHEDULED_AMOUNT = new BigDecimal("120");
	private static final BigDecimal UPDATED_USER_AMOUNT = new BigDecimal("4500");
	private static final BigDecimal SCHEDULED_BALANCE = new BigDecimal("4880");
	private static final BigDecimal USER_EXCHANGE_RATE = new BigDecimal("1.000000");
	private static final BigDecimal LATEST_ROW_EXCHANGE_RATE = new BigDecimal("1.234500");
	private static final BigDecimal SCHEDULED_EXCHANGE_RATE = new BigDecimal("1.085000");
	private static final Currency CURRENT_CURRENCY = Currency.EUR;
	private static final Currency CHANGED_CURRENCY = Currency.GBP;
	private static final Currency SCHEDULER_CURRENCY = Currency.USD;
	private static final String VERSION_FIELD = "version";
	private static final String ASSET_NOT_OWNED_MESSAGE = "This asset does not belong to you";
	private static final String SNAPSHOT_NOT_OWNED_MESSAGE = "This snapshot does not belong to you";
	private static final String SNAPSHOT_OUTSIDE_ASSET_MESSAGE =
		"This snapshot does not belong to the asset";

	@Mock
	private SnapshotRepository snapshotRepository;

	@Mock
	private AssetRepository assetRepository;

	@InjectMocks
	private SnapshotService testSubject;

	@Test
	void findAll_returnsSnapshotsFromRepository() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		List<Snapshot> stubbedSnapshots = Instancio.ofList(Snapshot.class).size(3).create();
		when(snapshotRepository.findByAssetUserId(mockUserId)).thenReturn(stubbedSnapshots);

		// WHEN
		List<Snapshot> expectedSnapshots = testSubject.findAll(mockUserId);

		// THEN
		assertThat(expectedSnapshots).isEqualTo(stubbedSnapshots);
	}

	@Test
	void findByAssetId_returnsSnapshots_whenAuthorized() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getUser), mockUser)
			.create();
		List<Snapshot> stubbedSnapshots = Instancio.ofList(Snapshot.class).size(2).create();
		when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));
		when(snapshotRepository.findByAssetId(mockAsset.getId())).thenReturn(stubbedSnapshots);

		// WHEN
		List<Snapshot> expectedSnapshots = testSubject.findByAssetId(mockAsset.getId(), mockUserId);

		// THEN
		assertThat(expectedSnapshots).isEqualTo(stubbedSnapshots);
	}

	@Test
	void findByAssetId_throwsNotFoundException_whenAssetNotFound() {
		// GIVEN
		UUID mockAssetId = UUID.randomUUID();
		UUID mockUserId = UUID.randomUUID();
		when(assetRepository.findById(mockAssetId)).thenReturn(Optional.empty());

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.findByAssetId(mockAssetId, mockUserId)).isInstanceOf(
			NotFoundException.class
		);
	}

	@Test
	void findByAssetId_throwsAccessDeniedException_whenAssetBelongsToDifferentUser() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UUID otherUserId = UUID.randomUUID();
		User mockOtherUser = Instancio.of(User.class)
			.set(Select.field(User::getId), otherUserId)
			.create();
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getUser), mockOtherUser)
			.create();
		when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.findByAssetId(mockAsset.getId(), mockUserId))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessage(ASSET_NOT_OWNED_MESSAGE);
	}

	@ParameterizedTest(name = "saveOrUpdate with {0}")
	@EnumSource(MonthState.class)
	void saveOrUpdate_stampsTheRowWithTheAssetCurrencyOfTheWrite(MonthState monthState) {
		// GIVEN
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getOriginalCurrency), CURRENT_CURRENCY)
			.create();
		Snapshot mockUserRow = buildRow(
			SnapshotSource.USER,
			USER_AMOUNT,
			REFERENCE_DATE,
			CHANGED_CURRENCY
		);
		Snapshot mockScheduledRow = buildRow(
			SnapshotSource.SCHEDULER,
			SCHEDULED_AMOUNT,
			SCHEDULED_REFERENCE_DATE,
			SCHEDULER_CURRENCY
		);
		stubMonthLookup(
			mockAsset,
			monthState == MonthState.USER_ROW ? Optional.of(mockUserRow) : Optional.empty(),
			monthState == MonthState.SCHEDULER_ROW ? Optional.of(mockScheduledRow) : Optional.empty()
		);
		when(snapshotRepository.save(notNull())).thenAnswer(invocation ->
			invocation.<Snapshot>getArgument(0)
		);

		// WHEN
		Snapshot expectedSnapshot = testSubject.saveOrUpdate(
			mockAsset,
			UPDATED_USER_AMOUNT,
			REFERENCE_DATE,
			USER_EXCHANGE_RATE
		);

		// THEN
		assertThat(expectedSnapshot.getSource()).isEqualTo(SnapshotSource.USER);
		assertThat(expectedSnapshot.getAsset()).isSameAs(mockAsset);
		assertThat(expectedSnapshot.getAmountOriginalCurrency()).isEqualByComparingTo(
			UPDATED_USER_AMOUNT
		);
		assertThat(expectedSnapshot.getReferenceDate()).isEqualTo(REFERENCE_DATE);
		assertThat(expectedSnapshot.getExchangeRateToBase()).isEqualByComparingTo(USER_EXCHANGE_RATE);
		assertThat(expectedSnapshot.getCurrency()).isEqualTo(CURRENT_CURRENCY);
		switch (monthState) {
			case NO_ROW -> assertThat(expectedSnapshot).isNotSameAs(mockUserRow);
			case USER_ROW -> assertThat(expectedSnapshot).isSameAs(mockUserRow);
			case SCHEDULER_ROW -> assertThat(expectedSnapshot).isNotSameAs(mockScheduledRow);
		}
	}

	@Test
	void saveOrUpdate_restampsTheUserRowAndLeavesTheScheduledRowOnItsOwnCurrency() {
		// GIVEN
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getOriginalCurrency), CURRENT_CURRENCY)
			.create();
		Snapshot mockUserRow = buildRow(
			SnapshotSource.USER,
			USER_AMOUNT,
			REFERENCE_DATE,
			CHANGED_CURRENCY
		);
		Snapshot mockScheduledRow = buildRow(
			SnapshotSource.SCHEDULER,
			SCHEDULED_AMOUNT,
			SCHEDULED_REFERENCE_DATE,
			SCHEDULER_CURRENCY
		);
		stubMonthLookup(mockAsset, Optional.of(mockUserRow), Optional.of(mockScheduledRow));
		when(snapshotRepository.save(notNull())).thenAnswer(invocation ->
			invocation.<Snapshot>getArgument(0)
		);

		// WHEN
		Snapshot expectedSnapshot = testSubject.saveOrUpdate(
			mockAsset,
			UPDATED_USER_AMOUNT,
			REFERENCE_DATE,
			USER_EXCHANGE_RATE
		);

		// THEN
		assertThat(expectedSnapshot).isSameAs(mockUserRow);
		assertThat(mockUserRow.getCurrency()).isEqualTo(CURRENT_CURRENCY);
		assertThat(mockScheduledRow.getCurrency()).isEqualTo(SCHEDULER_CURRENCY);
		assertThat(mockScheduledRow.getAmountOriginalCurrency()).isEqualByComparingTo(SCHEDULED_AMOUNT);
		assertThat(mockScheduledRow.getReferenceDate()).isEqualTo(SCHEDULED_REFERENCE_DATE);
	}

	@Test
	void saveOrUpdate_returnsTheLatestUserRow_whenTheAmountRepeatsInALaterMonth() {
		// GIVEN
		Asset mockAsset = buildAsset(CURRENT_CURRENCY);
		Snapshot mockLatestRow = buildRow(
			SnapshotSource.USER,
			LATEST_ROW_AMOUNT,
			REFERENCE_DATE,
			CURRENT_CURRENCY
		);
		stubLatestUserRow(mockAsset, Optional.of(mockLatestRow));

		// WHEN
		Snapshot expectedSnapshot = testSubject.saveOrUpdate(
			mockAsset,
			USER_AMOUNT,
			NEXT_MONTH_REFERENCE_DATE,
			USER_EXCHANGE_RATE
		);

		// THEN
		assertThat(expectedSnapshot).isSameAs(mockLatestRow);
		verify(snapshotRepository, never()).save(notNull());
	}

	@Test
	void saveOrUpdate_returnsTheLatestUserRow_whenTheDateIsALaterDayOfTheSameMonth() {
		// GIVEN
		Asset mockAsset = buildAsset(CURRENT_CURRENCY);
		Snapshot mockLatestRow = buildRow(
			SnapshotSource.USER,
			LATEST_ROW_AMOUNT,
			EARLIER_REFERENCE_DATE,
			CURRENT_CURRENCY
		);
		stubLatestUserRow(mockAsset, Optional.of(mockLatestRow));

		// WHEN
		Snapshot expectedSnapshot = testSubject.saveOrUpdate(
			mockAsset,
			USER_AMOUNT,
			REFERENCE_DATE,
			USER_EXCHANGE_RATE
		);

		// THEN
		assertThat(expectedSnapshot).isSameAs(mockLatestRow);
		verify(snapshotRepository, never()).save(notNull());
		verify(snapshotRepository, never()).findFirstByAssetIdAndSourceAndReferenceDateBetween(
			mockAsset.getId(),
			SnapshotSource.USER,
			MONTH_START,
			MONTH_END
		);
	}

	@Test
	void saveOrUpdate_writes_whenTheReferenceDateBackfillsBeforeTheLatestUserRow() {
		// GIVEN
		Asset mockAsset = buildAsset(CURRENT_CURRENCY);
		Snapshot mockLatestRow = buildRow(
			SnapshotSource.USER,
			LATEST_ROW_AMOUNT,
			LATER_REFERENCE_DATE,
			CURRENT_CURRENCY
		);
		stubLatestUserRow(mockAsset, Optional.of(mockLatestRow));
		stubUserMonthLookup(mockAsset, EARLIER_REFERENCE_DATE, Optional.of(mockLatestRow));
		stubSaveReturningInput();

		// WHEN
		Snapshot expectedSnapshot = testSubject.saveOrUpdate(
			mockAsset,
			USER_AMOUNT,
			EARLIER_REFERENCE_DATE,
			USER_EXCHANGE_RATE
		);

		// THEN
		assertThat(expectedSnapshot).isSameAs(mockLatestRow);
		assertThat(expectedSnapshot.getReferenceDate()).isEqualTo(EARLIER_REFERENCE_DATE);
	}

	@Test
	void saveOrUpdate_writesTheMonthRow_whenTheAmountChanged() {
		// GIVEN
		Asset mockAsset = buildAsset(CURRENT_CURRENCY);
		Snapshot mockMonthRow = buildRow(
			SnapshotSource.USER,
			LATEST_ROW_AMOUNT,
			EARLIER_REFERENCE_DATE,
			CURRENT_CURRENCY
		);
		stubLatestUserRow(mockAsset, Optional.of(mockMonthRow));
		stubUserMonthLookup(mockAsset, REFERENCE_DATE, Optional.of(mockMonthRow));
		stubSaveReturningInput();

		// WHEN
		Snapshot expectedSnapshot = testSubject.saveOrUpdate(
			mockAsset,
			UPDATED_USER_AMOUNT,
			REFERENCE_DATE,
			USER_EXCHANGE_RATE
		);

		// THEN
		assertThat(expectedSnapshot).isSameAs(mockMonthRow);
		assertThat(expectedSnapshot.getAmountOriginalCurrency()).isEqualByComparingTo(
			UPDATED_USER_AMOUNT
		);
	}

	@Test
	void saveOrUpdate_writes_whenTheLatestRowIsStampedInAnotherCurrency() {
		// GIVEN
		Asset mockAsset = buildAsset(CURRENT_CURRENCY);
		Snapshot mockLatestRow = buildRow(
			SnapshotSource.USER,
			LATEST_ROW_AMOUNT,
			EARLIER_REFERENCE_DATE,
			CHANGED_CURRENCY
		);
		stubLatestUserRow(mockAsset, Optional.of(mockLatestRow));
		stubUserMonthLookup(mockAsset, NEXT_MONTH_REFERENCE_DATE, Optional.empty());
		stubSaveReturningInput();

		// WHEN
		Snapshot expectedSnapshot = testSubject.saveOrUpdate(
			mockAsset,
			USER_AMOUNT,
			NEXT_MONTH_REFERENCE_DATE,
			USER_EXCHANGE_RATE
		);

		// THEN
		assertThat(expectedSnapshot).isNotSameAs(mockLatestRow);
		assertThat(expectedSnapshot.getCurrency()).isEqualTo(CURRENT_CURRENCY);
		assertThat(expectedSnapshot.getAmountOriginalCurrency()).isEqualByComparingTo(USER_AMOUNT);
	}

	@Test
	void saveOrUpdate_writesTheFirstSnapshot_whenNoUserRowExists() {
		// GIVEN
		Asset mockAsset = buildAsset(CURRENT_CURRENCY);
		stubLatestUserRow(mockAsset, Optional.empty());
		stubUserMonthLookup(mockAsset, REFERENCE_DATE, Optional.empty());
		stubSaveReturningInput();

		// WHEN
		Snapshot expectedSnapshot = testSubject.saveOrUpdate(
			mockAsset,
			USER_AMOUNT,
			REFERENCE_DATE,
			USER_EXCHANGE_RATE
		);

		// THEN
		assertThat(expectedSnapshot.getSource()).isEqualTo(SnapshotSource.USER);
		assertThat(expectedSnapshot.getAmountOriginalCurrency()).isEqualByComparingTo(USER_AMOUNT);
		assertThat(expectedSnapshot.getReferenceDate()).isEqualTo(REFERENCE_DATE);
		assertThat(expectedSnapshot.getCurrency()).isEqualTo(CURRENT_CURRENCY);
	}

	@Test
	void saveOrUpdate_leavesTheSkippedRowUntouched_whenTheAmountRepeats() {
		// GIVEN
		Asset mockAsset = buildAsset(CURRENT_CURRENCY);
		Snapshot mockLatestRow = Instancio.of(Snapshot.class)
			.set(Select.field(Snapshot::getAsset), mockAsset)
			.set(Select.field(Snapshot::getSource), SnapshotSource.USER)
			.set(Select.field(Snapshot::getAmountOriginalCurrency), LATEST_ROW_AMOUNT)
			.set(Select.field(Snapshot::getReferenceDate), REFERENCE_DATE)
			.set(Select.field(Snapshot::getCurrency), CURRENT_CURRENCY)
			.set(Select.field(Snapshot::getExchangeRateToBase), LATEST_ROW_EXCHANGE_RATE)
			.create();
		UUID expectedId = mockLatestRow.getId();
		LocalDate expectedReferenceDate = mockLatestRow.getReferenceDate();
		BigDecimal expectedAmount = mockLatestRow.getAmountOriginalCurrency();
		Currency expectedCurrency = mockLatestRow.getCurrency();
		SnapshotSource expectedSource = mockLatestRow.getSource();
		BigDecimal expectedExchangeRate = mockLatestRow.getExchangeRateToBase();
		Long expectedVersion = mockLatestRow.getVersion();
		stubLatestUserRow(mockAsset, Optional.of(mockLatestRow));

		// WHEN
		Snapshot expectedSnapshot = testSubject.saveOrUpdate(
			mockAsset,
			USER_AMOUNT,
			REFERENCE_DATE,
			USER_EXCHANGE_RATE
		);

		// THEN
		assertThat(expectedSnapshot).isSameAs(mockLatestRow);
		assertThat(expectedSnapshot.getId()).isEqualTo(expectedId);
		assertThat(expectedSnapshot.getReferenceDate()).isEqualTo(expectedReferenceDate);
		assertThat(expectedSnapshot.getAmountOriginalCurrency()).isEqualByComparingTo(expectedAmount);
		assertThat(expectedSnapshot.getCurrency()).isEqualTo(expectedCurrency);
		assertThat(expectedSnapshot.getSource()).isEqualTo(expectedSource);
		assertThat(expectedSnapshot.getExchangeRateToBase()).isEqualByComparingTo(expectedExchangeRate);
		assertThat(expectedSnapshot.getVersion()).isEqualTo(expectedVersion);
		verify(snapshotRepository, never()).save(notNull());
	}

	@Test
	void saveOrUpdate_writes_whenOnlyAScheduledRowNewerThanTheLatestUserRowRepeatsTheAmount() {
		// GIVEN
		Asset mockAsset = buildAsset(CURRENT_CURRENCY);
		Snapshot mockLatestUserRow = buildRow(
			SnapshotSource.USER,
			EARLIER_USER_AMOUNT,
			EARLIER_REFERENCE_DATE,
			CURRENT_CURRENCY
		);
		Snapshot mockNewerScheduledRow = buildRow(
			SnapshotSource.SCHEDULER,
			USER_AMOUNT,
			LATER_REFERENCE_DATE,
			CURRENT_CURRENCY
		);
		stubLatestUserRow(mockAsset, Optional.of(mockLatestUserRow));
		lenient()
			.when(snapshotRepository.findFirstByAssetIdOrderByReferenceDateDescIdAsc(mockAsset.getId()))
			.thenReturn(Optional.of(mockNewerScheduledRow));
		stubUserMonthLookup(mockAsset, LATER_REFERENCE_DATE, Optional.of(mockLatestUserRow));
		stubSaveReturningInput();

		// WHEN
		Snapshot expectedSnapshot = testSubject.saveOrUpdate(
			mockAsset,
			USER_AMOUNT,
			LATER_REFERENCE_DATE,
			USER_EXCHANGE_RATE
		);

		// THEN
		assertThat(expectedSnapshot).isSameAs(mockLatestUserRow);
		assertThat(expectedSnapshot).isNotSameAs(mockNewerScheduledRow);
		assertThat(expectedSnapshot.getAmountOriginalCurrency()).isEqualByComparingTo(USER_AMOUNT);
		assertThat(expectedSnapshot.getReferenceDate()).isEqualTo(LATER_REFERENCE_DATE);
	}

	@ParameterizedTest(name = "findExistingForMonth with {0}")
	@EnumSource(MonthState.class)
	void findExistingForMonth_matchesTheUserRowsOnly(MonthState monthState) {
		// GIVEN
		Asset mockAsset = Instancio.create(Asset.class);
		Snapshot mockUserRow = buildRow(
			SnapshotSource.USER,
			USER_AMOUNT,
			REFERENCE_DATE,
			CHANGED_CURRENCY
		);
		Snapshot mockScheduledRow = buildRow(
			SnapshotSource.SCHEDULER,
			SCHEDULED_AMOUNT,
			SCHEDULED_REFERENCE_DATE,
			SCHEDULER_CURRENCY
		);
		stubMonthLookup(
			mockAsset,
			monthState == MonthState.USER_ROW ? Optional.of(mockUserRow) : Optional.empty(),
			monthState == MonthState.SCHEDULER_ROW ? Optional.of(mockScheduledRow) : Optional.empty()
		);

		// WHEN
		Optional<Snapshot> expectedRow = testSubject.findExistingForMonth(
			mockAsset.getId(),
			REFERENCE_DATE
		);

		// THEN
		switch (monthState) {
			case USER_ROW -> assertThat(expectedRow).containsSame(mockUserRow);
			case NO_ROW, SCHEDULER_ROW -> assertThat(expectedRow).isEmpty();
		}
	}

	@Test
	void saveScheduledPayment_insertsASchedulerRowWithTheGivenDueDateAndRate() {
		// GIVEN
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getOriginalCurrency), CURRENT_CURRENCY)
			.create();
		when(snapshotRepository.save(notNull())).thenAnswer(invocation ->
			invocation.<Snapshot>getArgument(0)
		);

		// WHEN
		Snapshot expectedSnapshot = testSubject.saveScheduledPayment(
			mockAsset,
			SCHEDULED_AMOUNT,
			SCHEDULED_REFERENCE_DATE,
			SCHEDULED_EXCHANGE_RATE
		);

		// THEN
		assertThat(expectedSnapshot.getSource()).isEqualTo(SnapshotSource.SCHEDULER);
		assertThat(expectedSnapshot.getAsset()).isSameAs(mockAsset);
		assertThat(expectedSnapshot.getReferenceDate()).isEqualTo(SCHEDULED_REFERENCE_DATE);
		assertThat(expectedSnapshot.getExchangeRateToBase()).isEqualByComparingTo(
			SCHEDULED_EXCHANGE_RATE
		);
		assertThat(expectedSnapshot.getAmountOriginalCurrency()).isEqualByComparingTo(SCHEDULED_AMOUNT);
		assertThat(expectedSnapshot.getCurrency()).isEqualTo(CURRENT_CURRENCY);
	}

	@Test
	void saveScheduledPayment_insertsOnEveryCall_andNeverRewritesTheSameDate() {
		// GIVEN
		Asset mockAsset = Instancio.create(Asset.class);
		List<Snapshot> persistedRows = new ArrayList<>();
		when(snapshotRepository.save(notNull())).thenAnswer(invocation -> {
			Snapshot row = invocation.getArgument(0);
			persistedRows.add(row);
			return row;
		});

		// WHEN
		Snapshot expectedFirstRow = testSubject.saveScheduledPayment(
			mockAsset,
			SCHEDULED_AMOUNT,
			SCHEDULED_REFERENCE_DATE,
			SCHEDULED_EXCHANGE_RATE
		);
		Snapshot expectedSecondRow = testSubject.saveScheduledPayment(
			mockAsset,
			SCHEDULED_BALANCE,
			SCHEDULED_REFERENCE_DATE,
			SCHEDULED_EXCHANGE_RATE
		);

		// THEN
		assertThat(persistedRows).containsExactly(expectedFirstRow, expectedSecondRow);
		assertThat(persistedRows.get(0)).isNotSameAs(persistedRows.get(1));
		assertThat(persistedRows).allSatisfy(row -> {
			assertThat(row.getSource()).isEqualTo(SnapshotSource.SCHEDULER);
			assertThat(row.getReferenceDate()).isEqualTo(SCHEDULED_REFERENCE_DATE);
		});
	}

	@Test
	void deleteBulk_delegatesToRepository_whenAllSnapshotsAreAuthorized() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		UUID mockAssetId = UUID.randomUUID();
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getId), mockAssetId)
			.set(Select.field(Asset::getUser), mockUser)
			.create();
		Snapshot mockSnapshot = Instancio.of(Snapshot.class)
			.set(Select.field(Snapshot::getAsset), mockAsset)
			.create();
		List<UUID> mockIds = List.of(mockSnapshot.getId());
		when(snapshotRepository.findAllById(mockIds)).thenReturn(List.of(mockSnapshot));

		// WHEN
		testSubject.deleteBulk(mockIds, mockAssetId, mockUserId);

		// THEN
		verify(snapshotRepository).deleteAllInBatch(List.of(mockSnapshot));
	}

	@Test
	void deleteBulk_throwsAccessDeniedException_whenSnapshotBelongsToDifferentAsset() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		UUID mockAssetId = UUID.randomUUID();
		UUID differentAssetId = UUID.randomUUID();
		Asset mockDifferentAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getId), differentAssetId)
			.set(Select.field(Asset::getUser), mockUser)
			.create();
		Snapshot mockSnapshot = Instancio.of(Snapshot.class)
			.set(Select.field(Snapshot::getAsset), mockDifferentAsset)
			.create();
		List<UUID> mockIds = List.of(mockSnapshot.getId());
		when(snapshotRepository.findAllById(mockIds)).thenReturn(List.of(mockSnapshot));

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.deleteBulk(mockIds, mockAssetId, mockUserId))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessage(SNAPSHOT_OUTSIDE_ASSET_MESSAGE);
	}

	@Test
	void deleteBulk_throwsAccessDeniedException_whenSnapshotBelongsToDifferentUser() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UUID otherUserId = UUID.randomUUID();
		User mockOtherUser = Instancio.of(User.class)
			.set(Select.field(User::getId), otherUserId)
			.create();
		UUID mockAssetId = UUID.randomUUID();
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getId), mockAssetId)
			.set(Select.field(Asset::getUser), mockOtherUser)
			.create();
		Snapshot mockSnapshot = Instancio.of(Snapshot.class)
			.set(Select.field(Snapshot::getAsset), mockAsset)
			.create();
		List<UUID> mockIds = List.of(mockSnapshot.getId());
		when(snapshotRepository.findAllById(mockIds)).thenReturn(List.of(mockSnapshot));

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.deleteBulk(mockIds, mockAssetId, mockUserId))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessage(SNAPSHOT_NOT_OWNED_MESSAGE);
	}

	@Test
	void snapshot_declaresTheOptimisticLockVersion_soAConcurrentMonthRowWriteIsRejected()
		throws NoSuchFieldException {
		// GIVEN
		Field expectedVersionField = Snapshot.class.getDeclaredField(VERSION_FIELD);

		// WHEN
		boolean expectedVersionAnnotated = expectedVersionField.isAnnotationPresent(Version.class);

		// THEN
		assertThat(expectedVersionAnnotated).isTrue();
		assertThat(expectedVersionField.getType()).isEqualTo(Long.class);
	}

	private Asset buildAsset(Currency currency) {
		return Instancio.of(Asset.class)
			.set(Select.field(Asset::getOriginalCurrency), currency)
			.create();
	}

	private void stubLatestUserRow(Asset asset, Optional<Snapshot> userRow) {
		when(
			snapshotRepository.findFirstByAssetIdAndSourceOrderByReferenceDateDescIdAsc(
				asset.getId(),
				SnapshotSource.USER
			)
		).thenReturn(userRow);
	}

	private void stubUserMonthLookup(
		Asset asset,
		LocalDate referenceDate,
		Optional<Snapshot> userRow
	) {
		when(
			snapshotRepository.findFirstByAssetIdAndSourceAndReferenceDateBetween(
				asset.getId(),
				SnapshotSource.USER,
				referenceDate.withDayOfMonth(1),
				referenceDate.withDayOfMonth(referenceDate.lengthOfMonth())
			)
		).thenReturn(userRow);
	}

	private void stubSaveReturningInput() {
		when(snapshotRepository.save(notNull())).thenAnswer(invocation ->
			invocation.<Snapshot>getArgument(0)
		);
	}

	private void stubMonthLookup(
		Asset asset,
		Optional<Snapshot> userRow,
		Optional<Snapshot> scheduledRow
	) {
		when(
			snapshotRepository.findFirstByAssetIdAndSourceAndReferenceDateBetween(
				asset.getId(),
				SnapshotSource.USER,
				MONTH_START,
				MONTH_END
			)
		).thenReturn(userRow);
		lenient()
			.when(
				snapshotRepository.findFirstByAssetIdAndSourceAndReferenceDateBetween(
					asset.getId(),
					SnapshotSource.SCHEDULER,
					MONTH_START,
					MONTH_END
				)
			)
			.thenReturn(scheduledRow);
	}

	private Snapshot buildRow(
		SnapshotSource source,
		BigDecimal amount,
		LocalDate referenceDate,
		Currency currency
	) {
		return Instancio.of(Snapshot.class)
			.set(Select.field(Snapshot::getSource), source)
			.set(Select.field(Snapshot::getAmountOriginalCurrency), amount)
			.set(Select.field(Snapshot::getReferenceDate), referenceDate)
			.set(Select.field(Snapshot::getCurrency), currency)
			.create();
	}

	private enum MonthState {
		NO_ROW,
		USER_ROW,
		SCHEDULER_ROW
	}
}
