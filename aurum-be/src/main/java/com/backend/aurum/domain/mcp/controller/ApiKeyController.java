package com.backend.aurum.domain.mcp.controller;

import com.backend.aurum.domain.mcp.dto.ApiKeyMetaDTO;
import com.backend.aurum.domain.mcp.dto.GeneratedKeyResponseDTO;
import com.backend.aurum.domain.mcp.model.ApiKey;
import com.backend.aurum.domain.mcp.service.ApiKeyService;
import com.backend.aurum.domain.user.model.UserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/mcp")
@RequiredArgsConstructor
@Tag(name = "MCP", description = "MCP API key management")
public class ApiKeyController {

	private final ApiKeyService apiKeyService;

	@PostMapping("/keys")
	public ResponseEntity<GeneratedKeyResponseDTO> generateKey(@AuthenticationPrincipal UserPrincipal principal) {
		String plainKey = apiKeyService.generateKey(principal.user());
		return ResponseEntity.ok(new GeneratedKeyResponseDTO(plainKey));
	}

	@GetMapping("/keys")
	public ResponseEntity<ApiKeyMetaDTO> getKeyMeta(@AuthenticationPrincipal UserPrincipal principal) {
		Optional<ApiKey> key = apiKeyService.getKeyMeta(principal.user().getId());
		return key.map(k -> ResponseEntity.ok(new ApiKeyMetaDTO(k.getId(), k.getName(), k.getCreatedAt(), k.getLastUsedAt())))
				.orElse(ResponseEntity.notFound().build());
	}

	@DeleteMapping("/keys")
	public ResponseEntity<Void> revokeKey(@AuthenticationPrincipal UserPrincipal principal) {
		apiKeyService.revokeKey(principal.user().getId());
		return ResponseEntity.noContent().build();
	}
}
