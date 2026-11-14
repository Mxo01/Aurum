package com.backend.aurum.domain.asset.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.asset.dto.AssetDTO;
import com.backend.aurum.domain.asset.dto.CreateAssetDTO;
import com.backend.aurum.domain.asset.dto.SnapshotDTO;
import com.backend.aurum.domain.asset.dto.UpdateAssetDTO;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.AssetCategory;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.AssetCategoryRepository;
import com.backend.aurum.domain.shared.currency.CurrencyConversionService;
import com.backend.aurum.domain.shared.currency.RateBook;
import com.backend.aurum.domain.shared.exception.AccessDeniedException;
import com.backend.aurum.domain.shared.exception.NotFoundException;
import com.backend.aurum.domain.shared.port.ExchangeRateProvider;
import com.backend.aurum.domain.user.enums.Currency;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
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
class AssetMapperTest {

	private static final Currency USER_CURRENCY = Currency.USD;
	private static final Currency ASSET_CURRENCY = Currency.EUR;
	private static final LocalDate AS_OF = LocalDate.of(2026, 6, 15);
	private static final BigDecimal RATE_TO_USER_CURRENCY = new BigDecimal("1.5");
	private static final BigDecimal LATEST_AMOUNT = new BigDecimal("500");
	private static final BigDecimal PREVIOUS_AMOUNT = new BigDecimal("400");
	private static final BigDecimal CONVERTED_LATEST_AMOUNT = new BigDecimal("750");

	@Mock
	private AssetCategoryRepository categoryRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private SnapshotMapper snapshotMapper;

	@Mock
	private ExchangeRateProvider exchangeRateProvider;

	@InjectMocks
	private AssetMapper testSubject;

	@Test
	void toEntity_createDto_returnsNull_whenDtoIsNull() {
		// GIVEN / WHEN
		Asset result = testSubject.toEntity((CreateAssetDTO) null, UUID.randomUUID());

		// THEN
		assertThat(result).isNull();
	}

	@Test
	void toEntity_createDto_mapsFieldsResolvesUserAndCategory() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		AssetCategory mockCategory = Instancio.of(AssetCategory.class)
			.set(Select.field(AssetCategory::getUser), null)
			.create();
		CreateAssetDTO mockDto = Instancio.of(CreateAssetDTO.class)
			.set(Select.field(CreateAssetDTO::getCategoryId), mockCategory.getId())
			.set(Select.field(CreateAssetDTO::getOriginalCurrency), Currency.EUR)
			.create();
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));
		when(categoryRepository.findById(mockCategory.getId())).thenReturn(Optional.of(mockCategory));

		// WHEN
		Asset result = testSubject.toEntity(mockDto, mockUserId);

		// THEN
		assertThat(result.getName()).isEqualTo(mockDto.getName());
		assertThat(result.getOriginalCurrency()).isEqualTo(Currency.EUR);
		assertThat(result.getUser()).isEqualTo(mockUser);
		assertThat(result.getCategory()).isEqualTo(mockCategory);
	}

	@Test
	void toEntity_createDto_throwsNotFoundException_whenUserNotFound() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		CreateAssetDTO mockDto = Instancio.create(CreateAssetDTO.class);
		when(userRepository.findById(mockUserId)).thenReturn(Optional.empty());

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.toEntity(mockDto, mockUserId)).isInstanceOf(
			NotFoundException.class
		);
	}

	@Test
	void toEntity_createDto_throwsNotFoundException_whenCategoryNotFound() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.create(User.class);
		CreateAssetDTO mockDto = Instancio.create(CreateAssetDTO.class);
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));
		when(categoryRepository.findById(mockDto.getCategoryId())).thenReturn(Optional.empty());

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.toEntity(mockDto, mockUserId)).isInstanceOf(
			NotFoundException.class
		);
	}

	@Test
	void toEntity_createDto_throwsAccessDeniedException_whenCategoryBelongsToDifferentUser() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UUID otherUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		User mockOtherUser = Instancio.of(User.class)
			.set(Select.field(User::getId), otherUserId)
			.create();
		AssetCategory mockCategory = Instancio.of(AssetCategory.class)
			.set(Select.field(AssetCategory::getUser), mockOtherUser)
			.create();
		CreateAssetDTO mockDto = Instancio.of(CreateAssetDTO.class)
			.set(Select.field(CreateAssetDTO::getCategoryId), mockCategory.getId())
			.create();
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));
		when(categoryRepository.findById(mockCategory.getId())).thenReturn(Optional.of(mockCategory));

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.toEntity(mockDto, mockUserId)).isInstanceOf(
			AccessDeniedException.class
		);
	}

	@Test
	void toEntity_createDto_appliesDefaults_whenOptionalFieldsAreAbsent() {
		// GIVEN
		CreateAssetDTO mockDto = Instancio.of(CreateAssetDTO.class)
			.set(Select.field(CreateAssetDTO::getIsFavorite), null)
			.set(Select.field(CreateAssetDTO::getOriginalCurrency), null)
			.set(Select.field(CreateAssetDTO::getCategoryId), null)
			.create();

		// WHEN
		Asset result = testSubject.toEntity(mockDto, null);

		// THEN
		assertThat(result.getIsFavorite()).isFalse();
		assertThat(result.getOriginalCurrency()).isEqualTo(Currency.EUR);
		assertThat(result.getUser()).isNull();
		assertThat(result.getCategory()).isNull();
	}

	@Test
	void toEntity_createDto_throwsAccessDeniedException_whenCategoryIsOwned_andNoUserIsGiven() {
		// GIVEN
		AssetCategory mockCategory = Instancio.of(AssetCategory.class)
			.set(Select.field(AssetCategory::getUser), Instancio.create(User.class))
			.create();
		CreateAssetDTO mockDto = Instancio.of(CreateAssetDTO.class)
			.set(Select.field(CreateAssetDTO::getCategoryId), mockCategory.getId())
			.create();
		when(categoryRepository.findById(mockCategory.getId())).thenReturn(Optional.of(mockCategory));

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.toEntity(mockDto, null)).isInstanceOf(
			AccessDeniedException.class
		);
	}

	@Test
	void toEntity_createDto_acceptsTheCategory_whenItBelongsToTheUser() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		AssetCategory mockCategory = Instancio.of(AssetCategory.class)
			.set(Select.field(AssetCategory::getUser), mockUser)
			.create();
		CreateAssetDTO mockDto = Instancio.of(CreateAssetDTO.class)
			.set(Select.field(CreateAssetDTO::getCategoryId), mockCategory.getId())
			.create();
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));
		when(categoryRepository.findById(mockCategory.getId())).thenReturn(Optional.of(mockCategory));

		// WHEN
		Asset result = testSubject.toEntity(mockDto, mockUserId);

		// THEN
		assertThat(result.getCategory()).isEqualTo(mockCategory);
	}

	@Test
	void toEntity_updateDto_returnsNull_whenDtoIsNull() {
		// GIVEN / WHEN
		Asset result = testSubject.toEntity((UpdateAssetDTO) null, UUID.randomUUID());

		// THEN
		assertThat(result).isNull();
	}

	@Test
	void toEntity_updateDto_mapsFieldsResolvesUserAndCategory() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		AssetCategory mockCategory = Instancio.of(AssetCategory.class)
			.set(Select.field(AssetCategory::getUser), null)
			.create();
		UpdateAssetDTO mockDto = Instancio.of(UpdateAssetDTO.class)
			.set(Select.field(UpdateAssetDTO::getCategoryId), mockCategory.getId())
			.set(Select.field(UpdateAssetDTO::getOriginalCurrency), Currency.USD)
			.create();
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));
		when(categoryRepository.findById(mockCategory.getId())).thenReturn(Optional.of(mockCategory));

		// WHEN
		Asset result = testSubject.toEntity(mockDto, mockUserId);

		// THEN
		assertThat(result.getName()).isEqualTo(mockDto.getName());
		assertThat(result.getOriginalCurrency()).isEqualTo(Currency.USD);
		assertThat(result.getUser()).isEqualTo(mockUser);
		assertThat(result.getCategory()).isEqualTo(mockCategory);
	}

	@Test
	void toEntity_updateDto_appliesDefaults_whenOptionalFieldsAreAbsent() {
		// GIVEN
		UpdateAssetDTO mockDto = Instancio.of(UpdateAssetDTO.class)
			.set(Select.field(UpdateAssetDTO::getIsFavorite), null)
			.set(Select.field(UpdateAssetDTO::getOriginalCurrency), null)
			.set(Select.field(UpdateAssetDTO::getCategoryId), null)
			.create();

		// WHEN
		Asset result = testSubject.toEntity(mockDto, null);

		// THEN
		assertThat(result.getIsFavorite()).isFalse();
		assertThat(result.getOriginalCurrency()).isEqualTo(Currency.EUR);
		assertThat(result.getUser()).isNull();
		assertThat(result.getCategory()).isNull();
	}

	@Test
	void toEntity_updateDto_throwsAccessDeniedException_whenCategoryIsOwned_andNoUserIsGiven() {
		// GIVEN
		AssetCategory mockCategory = Instancio.of(AssetCategory.class)
			.set(Select.field(AssetCategory::getUser), Instancio.create(User.class))
			.create();
		UpdateAssetDTO mockDto = Instancio.of(UpdateAssetDTO.class)
			.set(Select.field(UpdateAssetDTO::getCategoryId), mockCategory.getId())
			.create();
		when(categoryRepository.findById(mockCategory.getId())).thenReturn(Optional.of(mockCategory));

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.toEntity(mockDto, null)).isInstanceOf(
			AccessDeniedException.class
		);
	}

	@Test
	void toEntity_updateDto_acceptsTheCategory_whenItBelongsToTheUser() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		AssetCategory mockCategory = Instancio.of(AssetCategory.class)
			.set(Select.field(AssetCategory::getUser), mockUser)
			.create();
		UpdateAssetDTO mockDto = Instancio.of(UpdateAssetDTO.class)
			.set(Select.field(UpdateAssetDTO::getCategoryId), mockCategory.getId())
			.create();
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));
		when(categoryRepository.findById(mockCategory.getId())).thenReturn(Optional.of(mockCategory));

		// WHEN
		Asset result = testSubject.toEntity(mockDto, mockUserId);

		// THEN
		assertThat(result.getCategory()).isEqualTo(mockCategory);
	}

	@Test
	void toEntity_updateDto_throwsNotFoundException_whenUserNotFound() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UpdateAssetDTO mockDto = Instancio.create(UpdateAssetDTO.class);
		when(userRepository.findById(mockUserId)).thenReturn(Optional.empty());

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.toEntity(mockDto, mockUserId)).isInstanceOf(
			NotFoundException.class
		);
	}

	@Test
	void toEntity_updateDto_throwsNotFoundException_whenCategoryNotFound() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.create(User.class);
		UpdateAssetDTO mockDto = Instancio.create(UpdateAssetDTO.class);
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));
		when(categoryRepository.findById(mockDto.getCategoryId())).thenReturn(Optional.empty());

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.toEntity(mockDto, mockUserId)).isInstanceOf(
			NotFoundException.class
		);
	}

	@Test
	void toEntity_updateDto_throwsAccessDeniedException_whenCategoryBelongsToDifferentUser() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UUID otherUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		User mockOtherUser = Instancio.of(User.class)
			.set(Select.field(User::getId), otherUserId)
			.create();
		AssetCategory mockCategory = Instancio.of(AssetCategory.class)
			.set(Select.field(AssetCategory::getUser), mockOtherUser)
			.create();
		UpdateAssetDTO mockDto = Instancio.of(UpdateAssetDTO.class)
			.set(Select.field(UpdateAssetDTO::getCategoryId), mockCategory.getId())
			.create();
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));
		when(categoryRepository.findById(mockCategory.getId())).thenReturn(Optional.of(mockCategory));

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.toEntity(mockDto, mockUserId)).isInstanceOf(
			AccessDeniedException.class
		);
	}

	@Test
	void toDto_mapsAllFieldsWithSnapshotsConvertedAtTheirReferenceDate() {
		// GIVEN
		AssetCategory mockCategory = Instancio.of(AssetCategory.class)
			.set(Select.field(AssetCategory::getUser), null)
			.create();
		Snapshot mockSnapshot = Instancio.create(Snapshot.class);
		SnapshotDTO stubbedSnapshotDto = Instancio.create(SnapshotDTO.class);
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getCategory), mockCategory)
			.set(Select.field(Asset::getSnapshots), new ArrayList<>(List.of(mockSnapshot)))
			.create();
		RateBook mockBook = stubBook(ASSET_CURRENCY, USER_CURRENCY, AS_OF);
		when(snapshotMapper.toDto(mockSnapshot, mockBook)).thenReturn(stubbedSnapshotDto);

		// WHEN
		AssetDTO result = testSubject.toDto(mockAsset, mockBook);

		// THEN
		assertThat(result.getId()).isEqualTo(mockAsset.getId());
		assertThat(result.getName()).isEqualTo(mockAsset.getName());
		assertThat(result.getCategoryId()).isEqualTo(mockCategory.getId());
		assertThat(result.getSnapshots()).containsExactly(stubbedSnapshotDto);
	}

	@Test
	void toDto_returnsNull_whenEntityIsNull() {
		// GIVEN
		RateBook mockBook = emptyBook(USER_CURRENCY);

		// WHEN
		AssetDTO result = testSubject.toDto(null, mockBook);

		// THEN
		assertThat(result).isNull();
	}

	@Test
	void toDto_leavesSnapshotsNull_whenEntityHasNoSnapshotCollection() {
		// GIVEN
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getCategory), null)
			.set(Select.field(Asset::getSnapshots), null)
			.create();
		RateBook mockBook = emptyBook(USER_CURRENCY);

		// WHEN
		AssetDTO result = testSubject.toDto(mockAsset, mockBook);

		// THEN
		assertThat(result.getSnapshots()).isNull();
	}

	@Test
	void toDto_returnsEmptySnapshots_whenEntityHasNone() {
		// GIVEN
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getCategory), null)
			.set(Select.field(Asset::getSnapshots), new ArrayList<Snapshot>())
			.create();
		RateBook mockBook = emptyBook(USER_CURRENCY);

		// WHEN
		AssetDTO result = testSubject.toDto(mockAsset, mockBook);

		// THEN
		assertThat(result.getSnapshots()).isEmpty();
	}

	@Test
	void toDtoLight_populatesLatestValueAndPreviousValue_convertedAtTheAsOfDate() {
		// GIVEN
		AssetCategory mockCategory = Instancio.of(AssetCategory.class)
			.set(Select.field(AssetCategory::getUser), null)
			.create();
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getCategory), mockCategory)
			.set(Select.field(Asset::getOriginalCurrency), ASSET_CURRENCY)
			.create();
		Snapshot mockLatestSnapshot = Instancio.of(Snapshot.class)
			.set(Select.field(Snapshot::getAmountOriginalCurrency), LATEST_AMOUNT)
			.create();
		Snapshot mockPreviousSnapshot = Instancio.of(Snapshot.class)
			.set(Select.field(Snapshot::getAmountOriginalCurrency), PREVIOUS_AMOUNT)
			.create();
		List<Snapshot> mockLatestTwo = List.of(mockLatestSnapshot, mockPreviousSnapshot);
		RateBook mockBook = stubBook(ASSET_CURRENCY, USER_CURRENCY, AS_OF);

		// WHEN
		AssetDTO result = testSubject.toDtoLight(mockAsset, mockLatestTwo, mockBook, AS_OF);

		// THEN
		assertThat(result.getLatestValue()).isEqualByComparingTo(LATEST_AMOUNT);
		assertThat(result.getLatestValueBase()).isEqualByComparingTo(CONVERTED_LATEST_AMOUNT);
		assertThat(result.getPreviousValue()).isEqualByComparingTo(PREVIOUS_AMOUNT);
	}

	@Test
	void toDtoLight_isIdentity_whenAssetUsesTheUserCurrency() {
		// GIVEN
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getCategory), null)
			.set(Select.field(Asset::getOriginalCurrency), USER_CURRENCY)
			.create();
		Snapshot mockLatestSnapshot = Instancio.of(Snapshot.class)
			.set(Select.field(Snapshot::getAmountOriginalCurrency), LATEST_AMOUNT)
			.create();
		RateBook mockBook = book(USER_CURRENCY, List.of(USER_CURRENCY), List.of(AS_OF));

		// WHEN
		AssetDTO result = testSubject.toDtoLight(
			mockAsset,
			List.of(mockLatestSnapshot),
			mockBook,
			AS_OF
		);

		// THEN
		assertThat(result.getLatestValueBase()).isEqualByComparingTo(LATEST_AMOUNT);
		assertThat(result.getPreviousValue()).isNull();
	}

	@Test
	void toDtoLight_leavesLatestValueNull_whenSnapshotListIsEmpty() {
		// GIVEN
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getCategory), null)
			.set(Select.field(Asset::getOriginalCurrency), ASSET_CURRENCY)
			.create();
		RateBook mockBook = emptyBook(USER_CURRENCY);

		// WHEN
		AssetDTO result = testSubject.toDtoLight(mockAsset, List.of(), mockBook, AS_OF);

		// THEN
		assertThat(result.getLatestValue()).isNull();
		assertThat(result.getLatestValueBase()).isNull();
		assertThat(result.getPreviousValue()).isNull();
	}

	@Test
	void toDtoLight_returnsNull_whenEntityIsNull() {
		// GIVEN
		RateBook mockBook = emptyBook(USER_CURRENCY);

		// WHEN
		AssetDTO result = testSubject.toDtoLight(null, List.of(), mockBook, AS_OF);

		// THEN
		assertThat(result).isNull();
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

	private RateBook emptyBook(Currency target) {
		return book(target, List.of(), List.of());
	}
}
