package com.backend.aurum.domain.cashflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.cashflow.model.CashFlow;
import com.backend.aurum.domain.cashflow.repository.CashFlowRepository;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CashFlowServiceTest {

	@Mock
	private CashFlowRepository cashFlowRepository;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private CashFlowService testSubject;

	@Test
	void getEntriesForYear_returnsEntriesFromRepository() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		Integer mockYear = 2025;
		List<CashFlow> stubbedEntries = Instancio.ofList(CashFlow.class).size(3).create();
		when(cashFlowRepository.findByUserIdAndYearOrderByMonthAsc(mockUserId, mockYear)).thenReturn(
			stubbedEntries
		);

		// WHEN
		List<CashFlow> expectedEntries = testSubject.getEntriesForYear(mockUserId, mockYear);

		// THEN
		assertThat(expectedEntries).isEqualTo(stubbedEntries);
	}

	@Test
	void getAvailableYears_returnsYearsFromRepository() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		List<Integer> stubbedYears = List.of(2023, 2024, 2025);
		when(cashFlowRepository.findDistinctYearsByUserId(mockUserId)).thenReturn(stubbedYears);

		// WHEN
		List<Integer> expectedYears = testSubject.getAvailableYears(mockUserId);

		// THEN
		assertThat(expectedYears).isEqualTo(stubbedYears);
	}

	@Test
	void upsertEntry_updatesExistingEntry_whenEntryExists() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		Integer mockYear = 2025;
		Integer mockMonth = 3;
		BigDecimal mockEarned = new BigDecimal("2000.00");
		BigDecimal mockSpent = new BigDecimal("800.00");
		CashFlow mockExisting = Instancio.of(CashFlow.class)
			.set(Select.field(CashFlow::getEarned), BigDecimal.ZERO)
			.set(Select.field(CashFlow::getSpent), BigDecimal.ZERO)
			.create();
		CashFlow stubbedSaved = Instancio.create(CashFlow.class);
		when(
			cashFlowRepository.findByUserIdAndYearAndMonth(mockUserId, mockYear, mockMonth)
		).thenReturn(Optional.of(mockExisting));
		when(cashFlowRepository.save(mockExisting)).thenReturn(stubbedSaved);

		// WHEN
		CashFlow expectedEntry = testSubject.upsertEntry(
			mockUserId,
			mockYear,
			mockMonth,
			mockEarned,
			mockSpent
		);

		// THEN
		assertThat(expectedEntry).isEqualTo(stubbedSaved);
		assertThat(mockExisting.getEarned()).isEqualByComparingTo(mockEarned);
		assertThat(mockExisting.getSpent()).isEqualByComparingTo(mockSpent);
	}

	@Test
	void upsertEntry_createsNewEntry_whenEntryDoesNotExist() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		Integer mockYear = 2025;
		Integer mockMonth = 5;
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		CashFlow stubbedSaved = Instancio.create(CashFlow.class);
		when(
			cashFlowRepository.findByUserIdAndYearAndMonth(mockUserId, mockYear, mockMonth)
		).thenReturn(Optional.empty());
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));
		when(cashFlowRepository.save(org.mockito.ArgumentMatchers.notNull())).thenReturn(stubbedSaved);

		// WHEN
		CashFlow expectedEntry = testSubject.upsertEntry(
			mockUserId,
			mockYear,
			mockMonth,
			new BigDecimal("1500.00"),
			new BigDecimal("600.00")
		);

		// THEN
		assertThat(expectedEntry).isEqualTo(stubbedSaved);
	}

	@Test
	void upsertEntry_throwsRuntimeException_whenUserNotFound_onNewEntry() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		when(cashFlowRepository.findByUserIdAndYearAndMonth(mockUserId, 2025, 1)).thenReturn(
			Optional.empty()
		);
		when(userRepository.findById(mockUserId)).thenReturn(Optional.empty());

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.upsertEntry(
				mockUserId,
				2025,
				1,
				new BigDecimal("1000.00"),
				new BigDecimal("500.00")
			)
		).isInstanceOf(RuntimeException.class);
	}

	@Test
	void upsertEntry_doesNotOverwriteEarned_whenEarnedIsNull() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		BigDecimal originalEarned = new BigDecimal("3000.00");
		CashFlow mockExisting = Instancio.of(CashFlow.class)
			.set(Select.field(CashFlow::getEarned), originalEarned)
			.create();
		CashFlow stubbedSaved = Instancio.create(CashFlow.class);
		when(cashFlowRepository.findByUserIdAndYearAndMonth(mockUserId, 2025, 1)).thenReturn(
			Optional.of(mockExisting)
		);
		when(cashFlowRepository.save(mockExisting)).thenReturn(stubbedSaved);

		// WHEN
		testSubject.upsertEntry(mockUserId, 2025, 1, null, new BigDecimal("400.00"));

		// THEN
		assertThat(mockExisting.getEarned()).isEqualByComparingTo(originalEarned);
	}

	@Test
	void upsertEntry_doesNotOverwriteSpent_whenSpentIsNull() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		BigDecimal originalSpent = new BigDecimal("1200.00");
		CashFlow mockExisting = Instancio.of(CashFlow.class)
			.set(Select.field(CashFlow::getSpent), originalSpent)
			.create();
		CashFlow stubbedSaved = Instancio.create(CashFlow.class);
		when(cashFlowRepository.findByUserIdAndYearAndMonth(mockUserId, 2025, 1)).thenReturn(
			Optional.of(mockExisting)
		);
		when(cashFlowRepository.save(mockExisting)).thenReturn(stubbedSaved);

		// WHEN
		testSubject.upsertEntry(mockUserId, 2025, 1, new BigDecimal("2000.00"), null);

		// THEN
		assertThat(mockExisting.getSpent()).isEqualByComparingTo(originalSpent);
	}
}
