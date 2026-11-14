package com.backend.aurum.domain.asset.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.model.SnapshotSource;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.asset.repository.SnapshotRepository;
import com.backend.aurum.domain.shared.exception.AccessDeniedException;
import com.backend.aurum.domain.shared.exception.NotFoundException;
import com.backend.aurum.domain.user.model.User;
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
	private static final LocalDate SCHEDULED_REFERENCE_DATE = LocalDate.of(2026, 3, 17);
	private static final LocalDate MONTH_START = LocalDate.of(2026, 3, 1);
	private static final LocalDate MONTH_END = LocalDate.of(2026, 3, 31);
	private static final BigDecimal USER_AMOUNT = new BigDecimal("5000");
	private static final BigDecimal SCHEDULED_AMOUNT = new BigDecimal("120");
	private static final BigDecimal UPDATED_USER_AMOUNT = new BigDecimal("4500");
	private static final BigDecimal SCHEDULED_BALANCE = new BigDecimal("4880");
	private static final BigDecimal USER_EXCHANGE_RATE = new BigDecimal("1.000000");
	private static final BigDecimal SCHEDULED_EXCHANGE_RATE = new BigDecimal("1.085000");

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
		assertThatThrownBy(() -> testSubject.findByAssetId(mockAsset.getId(), mockUserId)).isInstanceOf(
			AccessDeniedException.class
		);
	}

	@ParameterizedTest(name = "saveOrUpdate with {0}")
	@EnumSource(MonthState.class)
	void saveOrUpdate_persistsOnlyTheUserRowOfTheMonth(MonthState monthState) {
		// GIVEN
		Asset mockAsset = Instancio.create(Asset.class);
		Snapshot mockUserRow = buildRow(SnapshotSource.USER, USER_AMOUNT, REFERENCE_DATE);
		Snapshot mockScheduledRow = buildRow(
			SnapshotSource.SCHEDULER,
			SCHEDULED_AMOUNT,
			SCHEDULED_REFERENCE_DATE
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
		switch (monthState) {
			case NO_ROW -> assertThat(expectedSnapshot).isNotSameAs(mockUserRow);
			case USER_ROW -> assertThat(expectedSnapshot).isSameAs(mockUserRow);
			case SCHEDULER_ROW -> assertThat(expectedSnapshot).isNotSameAs(mockScheduledRow);
		}
		assertThat(mockScheduledRow.getSource()).isEqualTo(SnapshotSource.SCHEDULER);
		assertThat(mockScheduledRow.getAmountOriginalCurrency()).isEqualByComparingTo(SCHEDULED_AMOUNT);
		assertThat(mockScheduledRow.getReferenceDate()).isEqualTo(SCHEDULED_REFERENCE_DATE);
	}

	@ParameterizedTest(name = "findExistingForMonth with {0}")
	@EnumSource(MonthState.class)
	void findExistingForMonth_matchesTheUserRowsOnly(MonthState monthState) {
		// GIVEN
		Asset mockAsset = Instancio.create(Asset.class);
		Snapshot mockUserRow = buildRow(SnapshotSource.USER, USER_AMOUNT, REFERENCE_DATE);
		Snapshot mockScheduledRow = buildRow(
			SnapshotSource.SCHEDULER,
			SCHEDULED_AMOUNT,
			SCHEDULED_REFERENCE_DATE
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
		Asset mockAsset = Instancio.create(Asset.class);
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
		assertThatThrownBy(() -> testSubject.deleteBulk(mockIds, mockAssetId, mockUserId)).isInstanceOf(
			AccessDeniedException.class
		);
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
		assertThatThrownBy(() -> testSubject.deleteBulk(mockIds, mockAssetId, mockUserId)).isInstanceOf(
			AccessDeniedException.class
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

	private Snapshot buildRow(SnapshotSource source, BigDecimal amount, LocalDate referenceDate) {
		return Instancio.of(Snapshot.class)
			.set(Select.field(Snapshot::getSource), source)
			.set(Select.field(Snapshot::getAmountOriginalCurrency), amount)
			.set(Select.field(Snapshot::getReferenceDate), referenceDate)
			.create();
	}

	private enum MonthState {
		NO_ROW,
		USER_ROW,
		SCHEDULER_ROW
	}
}
