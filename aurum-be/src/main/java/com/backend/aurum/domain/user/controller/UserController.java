package com.backend.aurum.domain.user.controller;

import com.backend.aurum.domain.user.dto.UpdateCurrencyDTO;
import com.backend.aurum.domain.user.dto.UpdateLocaleDTO;
import com.backend.aurum.domain.user.dto.UpdateNameDTO;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.model.UserPrincipal;
import com.backend.aurum.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@GetMapping
	public ResponseEntity<User> getUser(@AuthenticationPrincipal UserPrincipal principal) {
		UUID userId = principal.user().getId();
		User user = userService.getUserById(userId);
		return ResponseEntity.ok(user);
	}

	@DeleteMapping
	public ResponseEntity<Void> deleteUser(@AuthenticationPrincipal UserPrincipal principal) {
		UUID userId = principal.user().getId();
		String jwtId = principal.jwt().getSubject();

		userService.deleteUser(userId, jwtId);

		return ResponseEntity.ok().build();
	}

	@PutMapping
	public ResponseEntity<Void> updateName(@AuthenticationPrincipal UserPrincipal principal,
			@RequestBody UpdateNameDTO dto) {
		String jwtId = principal.jwt().getSubject();

		userService.updateAuth0Name(jwtId, dto);

		return ResponseEntity.ok().build();
	}

	@PutMapping("/currency")
	public ResponseEntity<Void> updateCurrency(@AuthenticationPrincipal UserPrincipal principal,
			@RequestBody UpdateCurrencyDTO dto) {
		UUID userId = principal.user().getId();

		userService.updateCurrency(userId, dto);

		return ResponseEntity.ok().build();
	}

	@PutMapping("/locale")
	public ResponseEntity<Void> updateLocale(@AuthenticationPrincipal UserPrincipal principal,
			@RequestBody UpdateLocaleDTO dto) {
		UUID userId = principal.user().getId();

		userService.updateLocale(userId, dto);

		return ResponseEntity.ok().build();
	}
}
