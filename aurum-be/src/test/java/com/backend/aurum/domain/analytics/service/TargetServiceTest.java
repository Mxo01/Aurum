package com.backend.aurum.domain.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.analytics.model.Target;
import com.backend.aurum.domain.analytics.model.TargetType;
import com.backend.aurum.domain.analytics.repository.TargetRepository;
import com.backend.aurum.domain.user.model.User;
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
class TargetServiceTest {

	@Mock
	private TargetRepository targetRepository;

	@InjectMocks
	private TargetService testSubject;

	@Test
	void findAll_returnsTargetsFromRepository() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		BigDecimal mockNetWorth = BigDecimal.TEN;
		List<Target> stubbedTargets = Instancio.ofList(Target.class)
			.size(2)
			.set(Select.field(Target::getIsCompleted), false)
			.set(Select.field(Target::getTargetAmount), new BigDecimal("1000"))
			.set(Select.field(Target::getCurrentAmount), BigDecimal.ZERO)
			.create();
		when(targetRepository.findByUserId(mockUserId)).thenReturn(stubbedTargets);

		// WHEN
		List<Target> expectedTargets = testSubject.findAll(mockUserId, mockNetWorth);

		// THEN
		assertThat(expectedTargets).isEqualTo(stubbedTargets);
	}

	@Test
	void findById_returnsTarget_whenFound() {
		// GIVEN
		BigDecimal mockNetWorth = BigDecimal.TEN;
		Target mockTarget = Instancio.of(Target.class)
			.set(Select.field(Target::getIsCompleted), false)
			.set(Select.field(Target::getTargetAmount), new BigDecimal("1000"))
			.set(Select.field(Target::getCurrentAmount), BigDecimal.ZERO)
			.create();
		when(targetRepository.findById(mockTarget.getId())).thenReturn(Optional.of(mockTarget));

		// WHEN
		Target expectedTarget = testSubject.findById(mockTarget.getId(), mockNetWorth);

		// THEN
		assertThat(expectedTarget).isEqualTo(mockTarget);
	}

	@Test
	void findById_throwsRuntimeException_whenNotFound() {
		// GIVEN
		UUID mockId = UUID.randomUUID();
		BigDecimal mockNetWorth = BigDecimal.TEN;
		when(targetRepository.findById(mockId)).thenReturn(Optional.empty());

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.findById(mockId, mockNetWorth)).isInstanceOf(
			RuntimeException.class
		);
	}

	@Test
	void save_returnsSavedTarget() {
		// GIVEN
		BigDecimal mockNetWorth = BigDecimal.TEN;
		Target mockTarget = Instancio.of(Target.class)
			.set(Select.field(Target::getIsCompleted), false)
			.set(Select.field(Target::getTargetAmount), new BigDecimal("1000"))
			.set(Select.field(Target::getCurrentAmount), BigDecimal.ZERO)
			.create();
		Target stubbedSaved = Instancio.create(Target.class);
		when(targetRepository.save(mockTarget)).thenReturn(stubbedSaved);

		// WHEN
		Target expectedTarget = testSubject.save(mockTarget, mockNetWorth);

		// THEN
		assertThat(expectedTarget).isEqualTo(stubbedSaved);
	}

	@Test
	void save_marksTargetAsCompleted_whenNetWorthTargetReachedAmount() {
		// GIVEN
		BigDecimal mockNetWorth = new BigDecimal("1000");
		Target mockTarget = Instancio.of(Target.class)
			.set(Select.field(Target::getIsCompleted), false)
			.set(Select.field(Target::getTargetAmount), new BigDecimal("500"))
			.set(Select.field(Target::getType), TargetType.NET_WORTH)
			.create();
		when(targetRepository.save(mockTarget)).thenReturn(mockTarget);

		// WHEN
		testSubject.save(mockTarget, mockNetWorth);

		// THEN
		assertThat(mockTarget.getIsCompleted()).isTrue();
	}

	@Test
	void save_marksManualTargetAsCompleted_whenCurrentAmountReachedTargetAmount() {
		// GIVEN
		BigDecimal mockNetWorth = BigDecimal.ZERO;
		Target mockTarget = Instancio.of(Target.class)
			.set(Select.field(Target::getIsCompleted), false)
			.set(Select.field(Target::getTargetAmount), new BigDecimal("100"))
			.set(Select.field(Target::getCurrentAmount), new BigDecimal("200"))
			.set(Select.field(Target::getType), TargetType.MANUAL)
			.create();
		when(targetRepository.save(mockTarget)).thenReturn(mockTarget);

		// WHEN
		testSubject.save(mockTarget, mockNetWorth);

		// THEN
		assertThat(mockTarget.getIsCompleted()).isTrue();
	}

	@Test
	void update_returnsUpdatedTarget() {
		// GIVEN
		BigDecimal mockNetWorth = BigDecimal.TEN;
		Target mockTarget = Instancio.of(Target.class)
			.set(Select.field(Target::getIsCompleted), false)
			.set(Select.field(Target::getTargetAmount), new BigDecimal("1000"))
			.create();
		Target mockDetails = Instancio.of(Target.class)
			.set(Select.field(Target::getTargetAmount), new BigDecimal("2000"))
			.create();
		Target stubbedUpdated = Instancio.create(Target.class);
		when(targetRepository.findById(mockTarget.getId())).thenReturn(Optional.of(mockTarget));
		when(targetRepository.save(mockTarget)).thenReturn(stubbedUpdated);

		// WHEN
		Target expectedTarget = testSubject.update(mockTarget.getId(), mockDetails, mockNetWorth);

		// THEN
		assertThat(expectedTarget).isEqualTo(stubbedUpdated);
	}

	@Test
	void delete_delegatesToRepository_whenUserOwnsTarget() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		Target mockTarget = Instancio.of(Target.class)
			.set(Select.field(Target::getUser), mockUser)
			.create();
		when(targetRepository.findById(mockTarget.getId())).thenReturn(Optional.of(mockTarget));

		// WHEN
		testSubject.delete(mockTarget.getId(), mockUserId);

		// THEN
		verify(targetRepository).delete(mockTarget);
	}

	@Test
	void delete_throwsException_whenUserDoesNotOwnTarget() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UUID mockOtherUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class)
			.set(Select.field(User::getId), mockOtherUserId)
			.create();
		Target mockTarget = Instancio.of(Target.class)
			.set(Select.field(Target::getUser), mockUser)
			.create();
		when(targetRepository.findById(mockTarget.getId())).thenReturn(Optional.of(mockTarget));

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.delete(mockTarget.getId(), mockUserId)).isInstanceOf(
			IllegalArgumentException.class
		);
	}
}
