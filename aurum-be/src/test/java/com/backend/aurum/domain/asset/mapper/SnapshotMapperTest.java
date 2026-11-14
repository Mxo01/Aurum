package com.backend.aurum.domain.asset.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.asset.dto.CreateSnapshotDTO;
import com.backend.aurum.domain.asset.dto.SnapshotDTO;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.shared.currency.CurrencyConversionService;
import com.backend.aurum.domain.shared.currency.RateBook;
import com.backend.aurum.domain.shared.exception.AccessDeniedException;
import com.backend.aurum.domain.shared.exception.NotFoundException;
import com.backend.aurum.domain.shared.port.ExchangeRateProvider;
import com.backend.aurum.domain.user.enums.Currency;
import com.backend.aurum.domain.user.model.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
class SnapshotMapperTest {

	private static final Currency USER_CURRENCY = Currency.USD;
	private static final Currency ASSET_CURRENCY = Currency.EUR;
	private static final LocalDate REFERENCE_DATE = LocalDate.of(2026, 1, 31);
	private static final BigDecimal RATE_TO_USER_CURRENCY = new BigDecimal("1.5");
	private static final BigDecimal ORIGINAL_AMOUNT = new BigDecimal("100");
	private static final BigDecimal CONVERTED_AMOUNT = new BigDecimal("150");
	private static final BigDecimal STORED_RATE = new BigDecimal("2.0000");

	@Mock
	private AssetRepository assetRepository;

	@Mock
	private ExchangeRateProvider exchangeRateProvider;

	@InjectMocks
	private SnapshotMapper testSubject;

	@Test
	void toEntity_createDto_returnsNull_whenDtoIsNull() {
		// GIVEN / WHEN
		Snapshot result = testSubject.toEntity((CreateSnapshotDTO) null, UUID.randomUUID());

		// THEN
		assertThat(result).isNull();
	}

	@Test
	void toEntity_createDto_mapsFieldsAndResolvesAsset() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getUser), mockUser)
			.create();
		CreateSnapshotDTO mockDto = Instancio.of(CreateSnapshotDTO.class)
			.set(Select.field(CreateSnapshotDTO::getAssetId), mockAsset.getId())
			.set(Select.field(CreateSnapshotDTO::getExchangeRateToBase), RATE_TO_USER_CURRENCY)
			.create();
		when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));

		// WHEN
		Snapshot result = testSubject.toEntity(mockDto, mockUserId);

		// THEN
		assertThat(result.getReferenceDate()).isEqualTo(mockDto.getReferenceDate());
		assertThat(result.getAmountOriginalCurrency()).isEqualTo(mockDto.getAmountOriginalCurrency());
		assertThat(result.getExchangeRateToBase()).isEqualTo(mockDto.getExchangeRateToBase());
		assertThat(result.getAsset()).isEqualTo(mockAsset);
	}

	@Test
	void toEntity_createDto_defaultsExchangeRateToOne_whenNull() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getUser), mockUser)
			.create();
		CreateSnapshotDTO mockDto = Instancio.of(CreateSnapshotDTO.class)
			.set(Select.field(CreateSnapshotDTO::getAssetId), mockAsset.getId())
			.set(Select.field(CreateSnapshotDTO::getExchangeRateToBase), null)
			.create();
		when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));

		// WHEN
		Snapshot result = testSubject.toEntity(mockDto, mockUserId);

		// THEN
		assertThat(result.getExchangeRateToBase()).isEqualByComparingTo(BigDecimal.ONE);
	}

	@Test
	void toEntity_createDto_throwsNotFoundException_whenAssetNotFound() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		CreateSnapshotDTO mockDto = Instancio.create(CreateSnapshotDTO.class);
		when(assetRepository.findById(mockDto.getAssetId())).thenReturn(Optional.empty());

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.toEntity(mockDto, mockUserId)).isInstanceOf(
			NotFoundException.class
		);
	}

	@Test
	void toEntity_createDto_throwsAccessDeniedException_whenAssetBelongsToDifferentUser() {
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
		assertThatThrownBy(() -> testSubject.toEntity(mockDto, mockUserId)).isInstanceOf(
			AccessDeniedException.class
		);
	}

	@Test
	void toEntity_createDto_leavesAssetNull_whenDtoHasNoAssetId() {
		// GIVEN
		CreateSnapshotDTO mockDto = Instancio.of(CreateSnapshotDTO.class)
			.set(Select.field(CreateSnapshotDTO::getAssetId), null)
			.create();

		// WHEN
		Snapshot result = testSubject.toEntity(mockDto, UUID.randomUUID());

		// THEN
		assertThat(result.getAsset()).isNull();
	}

	@Test
	void toEntity_createDto_skipsOwnershipCheck_whenUserIdIsNull() {
		// GIVEN
		User mockOtherUser = Instancio.create(User.class);
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getUser), mockOtherUser)
			.create();
		CreateSnapshotDTO mockDto = Instancio.of(CreateSnapshotDTO.class)
			.set(Select.field(CreateSnapshotDTO::getAssetId), mockAsset.getId())
			.create();
		when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));

		// WHEN
		Snapshot result = testSubject.toEntity(mockDto, null);

		// THEN
		assertThat(result.getAsset()).isEqualTo(mockAsset);
	}

	@Test
	void toDto_returnsNull_whenEntityIsNull() {
		// GIVEN
		RateBook mockBook = book(USER_CURRENCY, List.of(), List.of());

		// WHEN
		SnapshotDTO result = testSubject.toDto(null, mockBook);

		// THEN
		assertThat(result).isNull();
	}

	@Test
	void toDto_convertsAmountAtTheSnapshotReferenceDate() {
		// GIVEN
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getOriginalCurrency), ASSET_CURRENCY)
			.create();
		Snapshot mockSnapshot = buildSnapshot(mockAsset);
		RateBook mockBook = stubBook(ASSET_CURRENCY, USER_CURRENCY, REFERENCE_DATE);

		// WHEN
		SnapshotDTO result = testSubject.toDto(mockSnapshot, mockBook);

		// THEN
		assertThat(result.getId()).isEqualTo(mockSnapshot.getId());
		assertThat(result.getReferenceDate()).isEqualTo(REFERENCE_DATE);
		assertThat(result.getAmountOriginalCurrency()).isEqualByComparingTo(ORIGINAL_AMOUNT);
		assertThat(result.getExchangeRateToBase()).isEqualByComparingTo(RATE_TO_USER_CURRENCY);
		assertThat(result.getAmountBaseCurrency()).isEqualByComparingTo(CONVERTED_AMOUNT);
		assertThat(result.getAssetId()).isEqualTo(mockAsset.getId());
		assertThat(result.getAssetName()).isEqualTo(mockAsset.getName());
	}

	@Test
	void toDto_isIdentity_whenAssetUsesTheUserCurrency() {
		// GIVEN
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getOriginalCurrency), USER_CURRENCY)
			.create();
		Snapshot mockSnapshot = buildSnapshot(mockAsset);
		RateBook mockBook = book(USER_CURRENCY, List.of(USER_CURRENCY), List.of(REFERENCE_DATE));

		// WHEN
		SnapshotDTO result = testSubject.toDto(mockSnapshot, mockBook);

		// THEN
		assertThat(result.getExchangeRateToBase()).isEqualByComparingTo(BigDecimal.ONE);
		assertThat(result.getAmountBaseCurrency()).isEqualByComparingTo(ORIGINAL_AMOUNT);
	}

	@Test
	void toDto_omitsAssetDetails_whenTheSnapshotResolvesWithoutAnAsset() {
		// GIVEN
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getOriginalCurrency), USER_CURRENCY)
			.create();
		Snapshot mockSnapshot = mock(Snapshot.class);
		when(mockSnapshot.getAsset()).thenReturn(mockAsset, null);
		when(mockSnapshot.getReferenceDate()).thenReturn(REFERENCE_DATE);
		when(mockSnapshot.getAmountOriginalCurrency()).thenReturn(ORIGINAL_AMOUNT);
		RateBook mockBook = book(USER_CURRENCY, List.of(USER_CURRENCY), List.of(REFERENCE_DATE));

		// WHEN
		SnapshotDTO result = testSubject.toDto(mockSnapshot, mockBook);

		// THEN
		assertThat(result.getAssetId()).isNull();
		assertThat(result.getAssetName()).isNull();
		assertThat(result.getAmountBaseCurrency()).isEqualByComparingTo(ORIGINAL_AMOUNT);
	}

	private Snapshot buildSnapshot(Asset asset) {
		return Instancio.of(Snapshot.class)
			.set(Select.field(Snapshot::getAsset), asset)
			.set(Select.field(Snapshot::getAmountOriginalCurrency), ORIGINAL_AMOUNT)
			.set(Select.field(Snapshot::getReferenceDate), REFERENCE_DATE)
			.set(Select.field(Snapshot::getExchangeRateToBase), STORED_RATE)
			.create();
	}

	private RateBook stubBook(Currency source, Currency target, LocalDate date) {
		when(
			exchangeRateProvider.getRates(source.getValue(), target.getValue(), List.of(date))
		).thenReturn(Map.of(date, RATE_TO_USER_CURRENCY));
		return book(target, List.of(source), List.of(date));
	}

	private RateBook book(Currency target, List<Currency> sources, List<LocalDate> dates) {
		return new CurrencyConversionService(exchangeRateProvider).book(target, sources, dates);
	}
}
