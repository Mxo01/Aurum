package com.backend.aurum.domain.mcp.controller;

import com.backend.aurum.domain.mcp.dto.ApiKeyMetaDTO;
import com.backend.aurum.domain.mcp.dto.GeneratedKeyResponseDTO;
import com.backend.aurum.domain.mcp.model.ApiKey;
import com.backend.aurum.domain.mcp.service.ApiKeyService;
import com.backend.aurum.domain.user.model.UserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
@Tag(name = "MCP", description = "MCP API key management")
public class ApiKeyController {

	private final ApiKeyService apiKeyService;

	@PostMapping("/keys")
	public ResponseEntity<GeneratedKeyResponseDTO> generateKey(
		@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID userId = principal.user().getId();
		log.info(
			"ApiKeyController#generateKey - Request to generate MCP API key for userId={}",
			userId
		);
		String plainKey = apiKeyService.generateKey(principal.user());
		log.info(
			"ApiKeyController#generateKey - MCP API key generated successfully for userId={}",
			userId
		);
		return ResponseEntity.ok(new GeneratedKeyResponseDTO(plainKey));
	}

	@GetMapping("/keys")
	public ResponseEntity<ApiKeyMetaDTO> getKeyMeta(
		@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID userId = principal.user().getId();
		log.debug("ApiKeyController#getKeyMeta - Request for API key metadata for userId={}", userId);
		Optional<ApiKey> key = apiKeyService.getKeyMeta(userId);
		return key
			.map(k ->
				ResponseEntity.ok(
					new ApiKeyMetaDTO(k.getId(), k.getName(), k.getCreatedAt(), k.getLastUsedAt())
				)
			)
			.orElse(ResponseEntity.notFound().build());
	}

	@DeleteMapping("/keys")
	public ResponseEntity<Void> revokeKey(@AuthenticationPrincipal UserPrincipal principal) {
		UUID userId = principal.user().getId();
		log.info("ApiKeyController#revokeKey - Request to revoke MCP API key for userId={}", userId);
		apiKeyService.revokeKey(userId);
		log.info("ApiKeyController#revokeKey - MCP API key revoked successfully for userId={}", userId);
		return ResponseEntity.noContent().build();
	}
}
