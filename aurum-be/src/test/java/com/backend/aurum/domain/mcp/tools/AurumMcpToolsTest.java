package com.backend.aurum.domain.mcp.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.analytics.dto.AnalyticsSummaryDTO;
import com.backend.aurum.domain.analytics.dto.CreateTargetDTO;
import com.backend.aurum.domain.analytics.dto.TargetDTO;
import com.backend.aurum.domain.analytics.dto.UpdateTargetDTO;
import com.backend.aurum.domain.analytics.facade.AnalyticsFacade;
import com.backend.aurum.domain.analytics.facade.TargetFacade;
import com.backend.aurum.domain.asset.dto.AssetCategoryDTO;
import com.backend.aurum.domain.asset.dto.AssetDTO;
import com.backend.aurum.domain.asset.dto.CreateAssetCategoryDTO;
import com.backend.aurum.domain.asset.dto.CreateAssetDTO;
import com.backend.aurum.domain.asset.dto.CreateSnapshotDTO;
import com.backend.aurum.domain.asset.dto.SnapshotDTO;
import com.backend.aurum.domain.asset.dto.UpdateAssetCategoryDTO;
import com.backend.aurum.domain.asset.dto.UpdateAssetDTO;
import com.backend.aurum.domain.asset.facade.AssetCategoryFacade;
import com.backend.aurum.domain.asset.facade.AssetFacade;
import com.backend.aurum.domain.asset.facade.SnapshotFacade;
import com.backend.aurum.domain.cashflow.dto.CashFlowEntryDTO;
import com.backend.aurum.domain.cashflow.dto.CashFlowYearDTO;
import com.backend.aurum.domain.cashflow.dto.UpdateCashFlowEntryDTO;
import com.backend.aurum.domain.cashflow.facade.CashFlowFacade;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.model.UserPrincipal;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AurumMcpToolsTest {

	@Mock
	private AssetFacade assetFacade;

	@Mock
	private SnapshotFacade snapshotFacade;

	@Mock
	private TargetFacade targetFacade;

	@Mock
	private AnalyticsFacade analyticsFacade;

	@Mock
	private AssetCategoryFacade categoryFacade;

	@Mock
	private CashFlowFacade cashFlowFacade;

	@InjectMocks
	private AurumMcpTools testSubject;

	private UUID mockUserId;

	@BeforeEach
	void setUpSecurityContext() {
		mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		UserPrincipal mockPrincipal = new UserPrincipal(mockUser, null);
		UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
			mockPrincipal,
			null,
			List.of()
		);
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void getAssets_delegatesToFacadeWithCurrentUser() {
		// GIVEN
		List<AssetDTO> stubbedDtos = Instancio.ofList(AssetDTO.class).size(2).create();
		when(assetFacade.getAllAssets(mockUserId)).thenReturn(stubbedDtos);

		// WHEN
		List<AssetDTO> expectedDtos = testSubject.getAssets();

		// THEN
		assertThat(expectedDtos).isEqualTo(stubbedDtos);
	}

	@Test
	void getSnapshots_delegatesToFacadeWithCurrentUser() {
		// GIVEN
		UUID mockAssetId = UUID.randomUUID();
		List<SnapshotDTO> stubbedDtos = Instancio.ofList(SnapshotDTO.class).size(2).create();
		when(snapshotFacade.getSnapshots(mockAssetId, mockUserId)).thenReturn(stubbedDtos);

		// WHEN
		List<SnapshotDTO> expectedDtos = testSubject.getSnapshots(mockAssetId.toString());

		// THEN
		assertThat(expectedDtos).isEqualTo(stubbedDtos);
	}

	@Test
	void getKpis_delegatesToFacadeWithCurrentUser() {
		// GIVEN
		AnalyticsSummaryDTO stubbedSummary = Instancio.create(AnalyticsSummaryDTO.class);
		when(analyticsFacade.getSummary(mockUserId)).thenReturn(stubbedSummary);

		// WHEN
		AnalyticsSummaryDTO expectedSummary = testSubject.getKpis();

		// THEN
		assertThat(expectedSummary).isEqualTo(stubbedSummary);
	}

	@Test
	void getTargets_delegatesToFacadeWithCurrentUser() {
		// GIVEN
		List<TargetDTO> stubbedDtos = Instancio.ofList(TargetDTO.class).size(2).create();
		when(targetFacade.getAllTargets(mockUserId)).thenReturn(stubbedDtos);

		// WHEN
		List<TargetDTO> expectedDtos = testSubject.getTargets();

		// THEN
		assertThat(expectedDtos).isEqualTo(stubbedDtos);
	}

	@Test
	void addAsset_delegatesToFacadeWithCurrentUser() {
		// GIVEN
		CreateAssetDTO mockDto = Instancio.create(CreateAssetDTO.class);
		AssetDTO stubbedDto = Instancio.create(AssetDTO.class);
		when(assetFacade.createAsset(mockDto, mockUserId)).thenReturn(stubbedDto);

		// WHEN
		AssetDTO expectedDto = testSubject.addAsset(mockDto);

		// THEN
		assertThat(expectedDto).isEqualTo(stubbedDto);
	}

	@Test
	void updateAsset_delegatesToFacadeWithCurrentUser() {
		// GIVEN
		UpdateAssetDTO mockDto = Instancio.create(UpdateAssetDTO.class);
		AssetDTO stubbedDto = Instancio.create(AssetDTO.class);
		when(assetFacade.updateAsset(mockDto.getId(), mockDto, mockUserId)).thenReturn(stubbedDto);

		// WHEN
		AssetDTO expectedDto = testSubject.updateAsset(mockDto);

		// THEN
		assertThat(expectedDto).isEqualTo(stubbedDto);
	}

	@Test
	void addSnapshot_delegatesToFacadeWithCurrentUser() {
		// GIVEN
		CreateSnapshotDTO mockDto = Instancio.create(CreateSnapshotDTO.class);
		SnapshotDTO stubbedDto = Instancio.create(SnapshotDTO.class);
		when(snapshotFacade.createSnapshot(mockDto, mockUserId)).thenReturn(stubbedDto);

		// WHEN
		SnapshotDTO expectedDto = testSubject.addSnapshot(mockDto);

		// THEN
		assertThat(expectedDto).isEqualTo(stubbedDto);
	}

	@Test
	void addTarget_delegatesToFacadeWithCurrentUser() {
		// GIVEN
		CreateTargetDTO mockDto = Instancio.create(CreateTargetDTO.class);
		TargetDTO stubbedDto = Instancio.create(TargetDTO.class);
		when(targetFacade.createTarget(mockDto, mockUserId)).thenReturn(stubbedDto);

		// WHEN
		TargetDTO expectedDto = testSubject.addTarget(mockDto);

		// THEN
		assertThat(expectedDto).isEqualTo(stubbedDto);
	}

	@Test
	void bulkAddAssets_processesAllDtosWithCurrentUser() {
		// GIVEN
		CreateAssetDTO mockDto1 = Instancio.create(CreateAssetDTO.class);
		CreateAssetDTO mockDto2 = Instancio.create(CreateAssetDTO.class);
		AssetDTO stubbedDto1 = Instancio.create(AssetDTO.class);
		AssetDTO stubbedDto2 = Instancio.create(AssetDTO.class);
		when(assetFacade.createAsset(mockDto1, mockUserId)).thenReturn(stubbedDto1);
		when(assetFacade.createAsset(mockDto2, mockUserId)).thenReturn(stubbedDto2);

		// WHEN
		List<AssetDTO> expectedDtos = testSubject.bulkAddAssets(List.of(mockDto1, mockDto2));

		// THEN
		assertThat(expectedDtos).containsExactly(stubbedDto1, stubbedDto2);
	}

	@Test
	void getCategories_delegatesToFacadeWithCurrentUser() {
		// GIVEN
		List<AssetCategoryDTO> stubbedDtos = Instancio.ofList(AssetCategoryDTO.class).size(3).create();
		when(categoryFacade.getAllCategories(mockUserId)).thenReturn(stubbedDtos);

		// WHEN
		List<AssetCategoryDTO> expectedDtos = testSubject.getCategories();

		// THEN
		assertThat(expectedDtos).isEqualTo(stubbedDtos);
	}

	@Test
	void addCategory_delegatesToFacadeWithCurrentUser() {
		// GIVEN
		CreateAssetCategoryDTO mockDto = Instancio.create(CreateAssetCategoryDTO.class);
		AssetCategoryDTO stubbedDto = Instancio.create(AssetCategoryDTO.class);
		when(categoryFacade.createCategory(mockDto, mockUserId)).thenReturn(stubbedDto);

		// WHEN
		AssetCategoryDTO expectedDto = testSubject.addCategory(mockDto);

		// THEN
		assertThat(expectedDto).isEqualTo(stubbedDto);
	}

	@Test
	void updateCategory_delegatesToFacadeWithCurrentUser() {
		// GIVEN
		UpdateAssetCategoryDTO mockDto = Instancio.create(UpdateAssetCategoryDTO.class);
		AssetCategoryDTO stubbedDto = Instancio.create(AssetCategoryDTO.class);
		when(categoryFacade.updateCategory(mockDto.getId(), mockDto, mockUserId)).thenReturn(
			stubbedDto
		);

		// WHEN
		AssetCategoryDTO expectedDto = testSubject.updateCategory(mockDto);

		// THEN
		assertThat(expectedDto).isEqualTo(stubbedDto);
	}

	@Test
	void bulkUpdateTargets_processesAllDtosWithCurrentUser() {
		// GIVEN
		List<UpdateTargetDTO> mockDtos = Instancio.ofList(UpdateTargetDTO.class).size(2).create();
		List<TargetDTO> stubbedDtos = Instancio.ofList(TargetDTO.class).size(2).create();
		when(targetFacade.bulkUpdateTargets(mockDtos, mockUserId)).thenReturn(stubbedDtos);

		// WHEN
		List<TargetDTO> expectedDtos = testSubject.bulkUpdateTargets(mockDtos);

		// THEN
		assertThat(expectedDtos).isEqualTo(stubbedDtos);
	}

	@Test
	void getCashFlowYear_delegatesToFacadeWithCurrentUser() {
		// GIVEN
		Integer mockYear = 2025;
		CashFlowYearDTO stubbedDto = Instancio.create(CashFlowYearDTO.class);
		when(cashFlowFacade.getYear(mockUserId, mockYear)).thenReturn(stubbedDto);

		// WHEN
		CashFlowYearDTO expectedDto = testSubject.getCashFlowYear(mockYear);

		// THEN
		assertThat(expectedDto).isEqualTo(stubbedDto);
	}

	@Test
	void updateCashFlowEntry_delegatesToFacadeWithCurrentUser() {
		// GIVEN
		Integer mockYear = 2025;
		Integer mockMonth = 6;
		UpdateCashFlowEntryDTO mockDto = Instancio.create(UpdateCashFlowEntryDTO.class);
		CashFlowEntryDTO stubbedDto = Instancio.create(CashFlowEntryDTO.class);
		when(cashFlowFacade.updateEntry(mockUserId, mockYear, mockMonth, mockDto)).thenReturn(
			stubbedDto
		);

		// WHEN
		CashFlowEntryDTO expectedDto = testSubject.updateCashFlowEntry(mockYear, mockMonth, mockDto);

		// THEN
		assertThat(expectedDto).isEqualTo(stubbedDto);
	}

	@Test
	void bulkUpdateCashFlowEntries_processesAllEntriesWithCurrentUser() {
		// GIVEN
		Integer mockYear = 2025;
		CashFlowEntryDTO mockEntry1 = Instancio.of(CashFlowEntryDTO.class)
			.set(Select.field(CashFlowEntryDTO::getMonth), 1)
			.set(Select.field(CashFlowEntryDTO::getEarned), new BigDecimal("1000.00"))
			.set(Select.field(CashFlowEntryDTO::getSpent), new BigDecimal("400.00"))
			.create();
		CashFlowEntryDTO mockEntry2 = Instancio.of(CashFlowEntryDTO.class)
			.set(Select.field(CashFlowEntryDTO::getMonth), 2)
			.set(Select.field(CashFlowEntryDTO::getEarned), new BigDecimal("1200.00"))
			.set(Select.field(CashFlowEntryDTO::getSpent), new BigDecimal("500.00"))
			.create();
		CashFlowEntryDTO stubbedResult1 = Instancio.create(CashFlowEntryDTO.class);
		CashFlowEntryDTO stubbedResult2 = Instancio.create(CashFlowEntryDTO.class);
		when(
			cashFlowFacade.updateEntry(
				org.mockito.ArgumentMatchers.eq(mockUserId),
				org.mockito.ArgumentMatchers.eq(mockYear),
				org.mockito.ArgumentMatchers.eq(1),
				org.mockito.ArgumentMatchers.notNull()
			)
		).thenReturn(stubbedResult1);
		when(
			cashFlowFacade.updateEntry(
				org.mockito.ArgumentMatchers.eq(mockUserId),
				org.mockito.ArgumentMatchers.eq(mockYear),
				org.mockito.ArgumentMatchers.eq(2),
				org.mockito.ArgumentMatchers.notNull()
			)
		).thenReturn(stubbedResult2);

		// WHEN
		List<CashFlowEntryDTO> expectedDtos = testSubject.bulkUpdateCashFlowEntries(
			mockYear,
			List.of(mockEntry1, mockEntry2)
		);

		// THEN
		assertThat(expectedDtos).containsExactly(stubbedResult1, stubbedResult2);
	}
}
