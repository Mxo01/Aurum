package com.backend.aurum.domain.analytics.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.analytics.dto.CreateTargetDTO;
import com.backend.aurum.domain.analytics.dto.TargetDTO;
import com.backend.aurum.domain.analytics.dto.UpdateTargetDTO;
import com.backend.aurum.domain.analytics.model.Target;
import com.backend.aurum.domain.analytics.model.TargetStatus;
import com.backend.aurum.domain.analytics.model.TargetType;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
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
class TargetMapperTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private TargetMapper testSubject;

	@Test
	void toEntity_createDto_returnsNull_whenDtoIsNull() {
		// GIVEN / WHEN
		Target result = testSubject.toEntity((CreateTargetDTO) null, UUID.randomUUID());

		// THEN
		assertThat(result).isNull();
	}

	@Test
	void toEntity_createDto_mapsFieldsAndResolvesUser() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		CreateTargetDTO mockDto = Instancio.of(CreateTargetDTO.class)
			.set(Select.field(CreateTargetDTO::getTargetAmount), BigDecimal.TEN)
			.set(Select.field(CreateTargetDTO::getType), TargetType.MANUAL)
			.create();
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));

		// WHEN
		Target result = testSubject.toEntity(mockDto, mockUserId);

		// THEN
		assertThat(result.getName()).isEqualTo(mockDto.getName());
		assertThat(result.getTargetAmount()).isEqualTo(mockDto.getTargetAmount());
		assertThat(result.getType()).isEqualTo(TargetType.MANUAL);
		assertThat(result.getIsCompleted()).isFalse();
		assertThat(result.getUser()).isEqualTo(mockUser);
	}

	@Test
	void toEntity_createDto_defaultsTypeToManual_whenTypeIsNull() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.create(User.class);
		CreateTargetDTO mockDto = Instancio.of(CreateTargetDTO.class)
			.set(Select.field(CreateTargetDTO::getType), null)
			.create();
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));

		// WHEN
		Target result = testSubject.toEntity(mockDto, mockUserId);

		// THEN
		assertThat(result.getType()).isEqualTo(TargetType.MANUAL);
	}

	@Test
	void toEntity_createDto_throwsRuntimeException_whenUserNotFound() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		CreateTargetDTO mockDto = Instancio.create(CreateTargetDTO.class);
		when(userRepository.findById(mockUserId)).thenReturn(Optional.empty());

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.toEntity(mockDto, mockUserId)).isInstanceOf(
			RuntimeException.class
		);
	}

	@Test
	void toEntity_updateDto_mapsFieldsAndResolvesUser() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		UpdateTargetDTO mockDto = Instancio.create(UpdateTargetDTO.class);
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));

		// WHEN
		Target result = testSubject.toEntity(mockDto, mockUserId);

		// THEN
		assertThat(result.getName()).isEqualTo(mockDto.getName());
		assertThat(result.getTargetAmount()).isEqualTo(mockDto.getTargetAmount());
		assertThat(result.getUser()).isEqualTo(mockUser);
	}

	@Test
	void toDto_returnsNull_whenEntityIsNull() {
		// GIVEN / WHEN
		TargetDTO result = testSubject.toDto(null, BigDecimal.TEN);

		// THEN
		assertThat(result).isNull();
	}

	@Test
	void toDto_setsCompletedStatusAndFullPercentage_whenTargetIsCompleted() {
		// GIVEN
		Target mockTarget = Instancio.of(Target.class)
			.set(Select.field(Target::getIsCompleted), true)
			.set(Select.field(Target::getTargetAmount), new BigDecimal("1000"))
			.set(Select.field(Target::getType), TargetType.MANUAL)
			.set(Select.field(Target::getCurrentAmount), new BigDecimal("1000"))
			.create();
		BigDecimal mockNetWorth = BigDecimal.TEN;

		// WHEN
		TargetDTO result = testSubject.toDto(mockTarget, mockNetWorth);

		// THEN
		assertThat(result.getIsCompleted()).isTrue();
		assertThat(result.getCompletionPercentage()).isEqualTo(100.0);
		assertThat(result.getStatus()).isEqualTo(TargetStatus.COMPLETED);
	}

	@Test
	void toDto_setsNotReachedStatus_whenDeadlineIsPast() {
		// GIVEN
		Target mockTarget = Instancio.of(Target.class)
			.set(Select.field(Target::getIsCompleted), false)
			.set(Select.field(Target::getTargetAmount), new BigDecimal("1000"))
			.set(Select.field(Target::getCurrentAmount), BigDecimal.ZERO)
			.set(Select.field(Target::getType), TargetType.MANUAL)
			.set(Select.field(Target::getDeadline), LocalDate.now().minusDays(1))
			.create();
		BigDecimal mockNetWorth = BigDecimal.TEN;

		// WHEN
		TargetDTO result = testSubject.toDto(mockTarget, mockNetWorth);

		// THEN
		assertThat(result.getStatus()).isEqualTo(TargetStatus.NOT_REACHED);
	}

	@Test
	void toDto_setsInProgressStatus_whenDeadlineIsInFuture() {
		// GIVEN
		Target mockTarget = Instancio.of(Target.class)
			.set(Select.field(Target::getIsCompleted), false)
			.set(Select.field(Target::getTargetAmount), new BigDecimal("1000"))
			.set(Select.field(Target::getCurrentAmount), BigDecimal.ZERO)
			.set(Select.field(Target::getType), TargetType.MANUAL)
			.set(Select.field(Target::getDeadline), LocalDate.now().plusYears(1))
			.create();
		BigDecimal mockNetWorth = BigDecimal.TEN;

		// WHEN
		TargetDTO result = testSubject.toDto(mockTarget, mockNetWorth);

		// THEN
		assertThat(result.getStatus()).isEqualTo(TargetStatus.IN_PROGRESS);
	}

	@Test
	void toDto_usesNetWorthAsCurrentAmount_forNetWorthTargetType() {
		// GIVEN
		BigDecimal mockNetWorth = new BigDecimal("500");
		Target mockTarget = Instancio.of(Target.class)
			.set(Select.field(Target::getIsCompleted), false)
			.set(Select.field(Target::getTargetAmount), new BigDecimal("1000"))
			.set(Select.field(Target::getType), TargetType.NET_WORTH)
			.set(Select.field(Target::getDeadline), LocalDate.now().plusYears(1))
			.create();

		// WHEN
		TargetDTO result = testSubject.toDto(mockTarget, mockNetWorth);

		// THEN
		assertThat(result.getCurrentAmount()).isEqualByComparingTo(mockNetWorth);
		assertThat(result.getCompletionPercentage()).isEqualTo(50.0);
	}

	@Test
	void toDto_floorsCurrentAmountAtZero_forNetWorthTargetWithNegativeNetWorth() {
		// GIVEN
		BigDecimal mockNegativeNetWorth = new BigDecimal("-500");
		Target mockTarget = Instancio.of(Target.class)
			.set(Select.field(Target::getIsCompleted), false)
			.set(Select.field(Target::getTargetAmount), new BigDecimal("1000"))
			.set(Select.field(Target::getType), TargetType.NET_WORTH)
			.set(Select.field(Target::getDeadline), LocalDate.now().plusYears(1))
			.create();

		// WHEN
		TargetDTO result = testSubject.toDto(mockTarget, mockNegativeNetWorth);

		// THEN
		assertThat(result.getCurrentAmount()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(result.getCompletionPercentage()).isEqualTo(0.0);
	}
}
