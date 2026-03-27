package com.backend.aurum.domain.asset.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.asset.dto.CreateSnapshotDTO;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.user.model.User;
import java.math.BigDecimal;
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
class SnapshotValidationServiceTest {

	@Mock
	private AssetRepository assetRepository;

	@InjectMocks
	private SnapshotValidationService testSubject;

	@Test
	void validate_throwsWhenDtoIsNull() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.validate((CreateSnapshotDTO) null, mockUserId)
		).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void validate_throwsWhenAssetIdIsNull() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		CreateSnapshotDTO mockDto = Instancio.of(CreateSnapshotDTO.class)
			.set(Select.field(CreateSnapshotDTO::getAssetId), null)
			.create();

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto, mockUserId)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	@Test
	void validate_throwsWhenReferenceDateIsNull() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		CreateSnapshotDTO mockDto = Instancio.of(CreateSnapshotDTO.class)
			.set(Select.field(CreateSnapshotDTO::getReferenceDate), null)
			.create();

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto, mockUserId)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	@Test
	void validate_throwsWhenAmountIsNull() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		CreateSnapshotDTO mockDto = Instancio.of(CreateSnapshotDTO.class)
			.set(Select.field(CreateSnapshotDTO::getAmountOriginalCurrency), null)
			.create();

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto, mockUserId)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	@Test
	void validate_throwsWhenExchangeRateIsNegative() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		CreateSnapshotDTO mockDto = Instancio.of(CreateSnapshotDTO.class)
			.set(Select.field(CreateSnapshotDTO::getExchangeRateToBase), new BigDecimal("-0.5"))
			.create();

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto, mockUserId)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	@Test
	void validate_throwsWhenExchangeRateIsZero() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		CreateSnapshotDTO mockDto = Instancio.of(CreateSnapshotDTO.class)
			.set(Select.field(CreateSnapshotDTO::getExchangeRateToBase), BigDecimal.ZERO)
			.create();

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto, mockUserId)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	@Test
	void validate_throwsWhenAssetNotFound() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		CreateSnapshotDTO mockDto = Instancio.create(CreateSnapshotDTO.class);
		when(assetRepository.findById(mockDto.getAssetId())).thenReturn(Optional.empty());

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto, mockUserId)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	@Test
	void validate_throwsWhenAssetBelongsToDifferentUser() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UUID otherUserId = UUID.randomUUID();
		User mockOtherUser = Instancio.of(User.class)
			.set(Select.field(User::getId), otherUserId)
			.create();
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getUser), mockOtherUser)
			.create();
		CreateSnapshotDTO mockDto = Instancio.of(CreateSnapshotDTO.class)
			.set(Select.field(CreateSnapshotDTO::getAssetId), mockAsset.getId())
			.create();
		when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto, mockUserId)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	@Test
	void validate_doesNotThrowForValidDto() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getUser), mockUser)
			.create();
		CreateSnapshotDTO mockDto = Instancio.of(CreateSnapshotDTO.class)
			.set(Select.field(CreateSnapshotDTO::getAssetId), mockAsset.getId())
			.set(Select.field(CreateSnapshotDTO::getExchangeRateToBase), new BigDecimal("1.5"))
			.create();
		when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));

		// WHEN / THEN
		assertThatCode(() -> testSubject.validate(mockDto, mockUserId)).doesNotThrowAnyException();
	}
}
