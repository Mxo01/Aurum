package com.backend.aurum.domain.analytics.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.analytics.dto.CreateTargetDTO;
import com.backend.aurum.domain.analytics.dto.TargetDTO;
import com.backend.aurum.domain.analytics.dto.UpdateTargetDTO;
import com.backend.aurum.domain.analytics.mapper.TargetMapper;
import com.backend.aurum.domain.analytics.model.Target;
import com.backend.aurum.domain.analytics.service.AnalyticsService;
import com.backend.aurum.domain.analytics.service.TargetService;
import com.backend.aurum.domain.analytics.validation.TargetValidationService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TargetFacadeTest {

	@Mock
	private TargetService targetService;

	@Mock
	private TargetMapper mapper;

	@Mock
	private TargetValidationService validationService;

	@Mock
	private AnalyticsService analyticsService;

	@InjectMocks
	private TargetFacade testSubject;

	@Test
	void createTarget_validatesAndReturnsDto() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		BigDecimal mockNetWorth = BigDecimal.TEN;
		CreateTargetDTO mockDto = Instancio.create(CreateTargetDTO.class);
		Target mockTarget = Instancio.create(Target.class);
		Target mockSaved = Instancio.create(Target.class);
		TargetDTO stubbedDto = Instancio.create(TargetDTO.class);
		when(analyticsService.getNetWorth(mockUserId)).thenReturn(mockNetWorth);
		when(mapper.toEntity(mockDto, mockUserId)).thenReturn(mockTarget);
		when(targetService.save(mockTarget, mockNetWorth)).thenReturn(mockSaved);
		when(mapper.toDto(mockSaved, mockNetWorth)).thenReturn(stubbedDto);

		// WHEN
		TargetDTO expectedDto = testSubject.createTarget(mockDto, mockUserId);

		// THEN
		assertThat(expectedDto).isEqualTo(stubbedDto);
		verify(validationService).validate(mockDto);
	}

	@Test
	void updateTarget_validatesAndReturnsDto() {
		// GIVEN
		UUID mockId = UUID.randomUUID();
		UUID mockUserId = UUID.randomUUID();
		BigDecimal mockNetWorth = BigDecimal.TEN;
		UpdateTargetDTO mockDto = Instancio.create(UpdateTargetDTO.class);
		Target mockDetails = Instancio.create(Target.class);
		Target mockUpdated = Instancio.create(Target.class);
		TargetDTO stubbedDto = Instancio.create(TargetDTO.class);
		when(analyticsService.getNetWorth(mockUserId)).thenReturn(mockNetWorth);
		when(mapper.toEntity(mockDto, mockUserId)).thenReturn(mockDetails);
		when(targetService.update(mockId, mockDetails, mockNetWorth)).thenReturn(mockUpdated);
		when(mapper.toDto(mockUpdated, mockNetWorth)).thenReturn(stubbedDto);

		// WHEN
		TargetDTO expectedDto = testSubject.updateTarget(mockId, mockDto, mockUserId);

		// THEN
		assertThat(expectedDto).isEqualTo(stubbedDto);
		verify(validationService).validate(mockDto);
	}

	@Test
	void deleteTarget_delegatesToService() {
		// GIVEN
		UUID mockId = UUID.randomUUID();
		UUID mockUserId = UUID.randomUUID();

		// WHEN
		testSubject.deleteTarget(mockId, mockUserId);

		// THEN
		verify(targetService).delete(mockId, mockUserId);
	}

	@Test
	void bulkCreateTargets_validatesEachAndReturnsAllDtos() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		BigDecimal mockNetWorth = BigDecimal.TEN;
		CreateTargetDTO mockDto1 = Instancio.create(CreateTargetDTO.class);
		CreateTargetDTO mockDto2 = Instancio.create(CreateTargetDTO.class);
		Target mockTarget1 = Instancio.create(Target.class);
		Target mockTarget2 = Instancio.create(Target.class);
		Target mockSaved1 = Instancio.create(Target.class);
		Target mockSaved2 = Instancio.create(Target.class);
		TargetDTO stubbedDto1 = Instancio.create(TargetDTO.class);
		TargetDTO stubbedDto2 = Instancio.create(TargetDTO.class);
		when(analyticsService.getNetWorth(mockUserId)).thenReturn(mockNetWorth);
		when(mapper.toEntity(mockDto1, mockUserId)).thenReturn(mockTarget1);
		when(mapper.toEntity(mockDto2, mockUserId)).thenReturn(mockTarget2);
		when(targetService.save(mockTarget1, mockNetWorth)).thenReturn(mockSaved1);
		when(targetService.save(mockTarget2, mockNetWorth)).thenReturn(mockSaved2);
		when(mapper.toDto(mockSaved1, mockNetWorth)).thenReturn(stubbedDto1);
		when(mapper.toDto(mockSaved2, mockNetWorth)).thenReturn(stubbedDto2);

		// WHEN
		List<TargetDTO> expectedDtos = testSubject.bulkCreateTargets(
			List.of(mockDto1, mockDto2),
			mockUserId
		);

		// THEN
		assertThat(expectedDtos).containsExactly(stubbedDto1, stubbedDto2);
		verify(validationService).validate(mockDto1);
		verify(validationService).validate(mockDto2);
	}

	@Test
	void bulkUpdateTargets_validatesEachAndReturnsAllDtos() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		BigDecimal mockNetWorth = BigDecimal.TEN;
		UpdateTargetDTO mockDto1 = Instancio.create(UpdateTargetDTO.class);
		UpdateTargetDTO mockDto2 = Instancio.create(UpdateTargetDTO.class);
		Target mockDetails1 = Instancio.create(Target.class);
		Target mockDetails2 = Instancio.create(Target.class);
		Target mockUpdated1 = Instancio.create(Target.class);
		Target mockUpdated2 = Instancio.create(Target.class);
		TargetDTO stubbedDto1 = Instancio.create(TargetDTO.class);
		TargetDTO stubbedDto2 = Instancio.create(TargetDTO.class);
		when(analyticsService.getNetWorth(mockUserId)).thenReturn(mockNetWorth);
		when(mapper.toEntity(mockDto1, mockUserId)).thenReturn(mockDetails1);
		when(mapper.toEntity(mockDto2, mockUserId)).thenReturn(mockDetails2);
		when(targetService.update(mockDto1.getId(), mockDetails1, mockNetWorth)).thenReturn(
			mockUpdated1
		);
		when(targetService.update(mockDto2.getId(), mockDetails2, mockNetWorth)).thenReturn(
			mockUpdated2
		);
		when(mapper.toDto(mockUpdated1, mockNetWorth)).thenReturn(stubbedDto1);
		when(mapper.toDto(mockUpdated2, mockNetWorth)).thenReturn(stubbedDto2);

		// WHEN
		List<TargetDTO> expectedDtos = testSubject.bulkUpdateTargets(
			List.of(mockDto1, mockDto2),
			mockUserId
		);

		// THEN
		assertThat(expectedDtos).containsExactly(stubbedDto1, stubbedDto2);
		verify(validationService).validate(mockDto1);
		verify(validationService).validate(mockDto2);
	}
}
