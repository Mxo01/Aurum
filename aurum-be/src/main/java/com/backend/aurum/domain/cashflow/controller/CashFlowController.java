package com.backend.aurum.domain.cashflow.controller;

import com.backend.aurum.domain.cashflow.dto.CashFlowEntryDTO;
import com.backend.aurum.domain.cashflow.dto.CashFlowYearDTO;
import com.backend.aurum.domain.cashflow.dto.UpdateCashFlowEntryDTO;
import com.backend.aurum.domain.cashflow.facade.CashFlowFacade;
import com.backend.aurum.domain.user.model.UserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/cashflows")
@RequiredArgsConstructor
@Tag(name = "Cash Flow", description = "Monthly cash flow management")
public class CashFlowController {

	private final CashFlowFacade cashFlowFacade;

	@GetMapping("/{year}")
	public ResponseEntity<CashFlowYearDTO> getYear(
		@PathVariable Integer year,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID userId = principal.user().getId();
		log.debug(
			"CashFlowController#getYear - Request to get cash flow for userId={}, year={}",
			userId,
			year
		);
		return ResponseEntity.ok(cashFlowFacade.getYear(userId, year));
	}

	@PutMapping("/{year}/{month}")
	public ResponseEntity<CashFlowEntryDTO> updateEntry(
		@PathVariable Integer year,
		@PathVariable Integer month,
		@RequestBody UpdateCashFlowEntryDTO dto,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID userId = principal.user().getId();
		log.info(
			"CashFlowController#updateEntry - Request to update cash flow for userId={}, year={}, month={}",
			userId,
			year,
			month
		);
		CashFlowEntryDTO result = cashFlowFacade.updateEntry(userId, year, month, dto);
		log.info("CashFlowController#updateEntry - Cash flow entry updated: id={}", result.getId());
		return ResponseEntity.ok(result);
	}
}
