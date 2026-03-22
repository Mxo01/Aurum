package com.backend.aurum.domain.user.controller;

import com.backend.aurum.domain.user.dto.UpdateCurrencyDTO;
import com.backend.aurum.domain.user.dto.UpdateLocaleDTO;
import com.backend.aurum.domain.user.dto.UpdateNameDTO;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.model.UserPrincipal;
import com.backend.aurum.domain.user.service.UserService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@GetMapping
	public ResponseEntity<User> getUser(@AuthenticationPrincipal UserPrincipal principal) {
		UUID userId = principal.user().getId();
		log.debug("UserController#getUser - Request to fetch user profile for userId={}", userId);
		User user = userService.getUserById(userId);
		return ResponseEntity.ok(user);
	}

	@DeleteMapping
	public ResponseEntity<Void> deleteUser(@AuthenticationPrincipal UserPrincipal principal) {
		UUID userId = principal.user().getId();
		String jwtId = principal.jwt().getSubject();
		log.info("UserController#deleteUser - Request to delete user: userId={}", userId);

		userService.deleteUser(userId, jwtId);
		log.info("UserController#deleteUser - User deleted successfully: userId={}", userId);

		return ResponseEntity.ok().build();
	}

	@PutMapping
	public ResponseEntity<Void> updateName(
		@AuthenticationPrincipal UserPrincipal principal,
		@RequestBody UpdateNameDTO dto
	) {
		String jwtId = principal.jwt().getSubject();
		log.info("UserController#updateName - Request to update name for jwtId={}", jwtId);

		userService.updateAuth0Name(jwtId, dto);
		log.info("UserController#updateName - Name updated successfully for jwtId={}", jwtId);

		return ResponseEntity.ok().build();
	}

	@PutMapping("/currency")
	public ResponseEntity<Void> updateCurrency(
		@AuthenticationPrincipal UserPrincipal principal,
		@RequestBody UpdateCurrencyDTO dto
	) {
		UUID userId = principal.user().getId();
		log.info(
			"UserController#updateCurrency - Request to update currency for userId={}, newCurrency={}",
			userId,
			dto.currency()
		);

		userService.updateCurrency(userId, dto);

		return ResponseEntity.ok().build();
	}

	@PutMapping("/locale")
	public ResponseEntity<Void> updateLocale(
		@AuthenticationPrincipal UserPrincipal principal,
		@RequestBody UpdateLocaleDTO dto
	) {
		UUID userId = principal.user().getId();
		log.info(
			"UserController#updateLocale - Request to update locale for userId={}, newLocale={}",
			userId,
			dto.locale()
		);

		userService.updateLocale(userId, dto);

		return ResponseEntity.ok().build();
	}

	@PostMapping(value = "/picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Void> updatePicture(
		@AuthenticationPrincipal UserPrincipal principal,
		@RequestParam("file") MultipartFile file
	) {
		UUID userId = principal.user().getId();
		if (userService.isGoogleUser(principal)) {
			log.warn(
				"UserController#updatePicture - Google user attempted to upload a picture, blocked: userId={}",
				userId
			);
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		log.info(
			"UserController#updatePicture - Request to update profile picture for userId={}",
			userId
		);
		userService.updatePicture(userId, file);

		return ResponseEntity.ok().build();
	}

	@DeleteMapping("/picture")
	public ResponseEntity<Void> deletePicture(@AuthenticationPrincipal UserPrincipal principal) {
		UUID userId = principal.user().getId();
		if (userService.isGoogleUser(principal)) {
			log.warn(
				"UserController#deletePicture - Google user attempted to delete a picture, blocked: userId={}",
				userId
			);
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		log.info(
			"UserController#deletePicture - Request to delete profile picture for userId={}",
			userId
		);
		userService.deletePicture(userId);

		return ResponseEntity.ok().build();
	}
}
