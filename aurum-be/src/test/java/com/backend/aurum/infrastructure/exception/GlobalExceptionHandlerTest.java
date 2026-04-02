package com.backend.aurum.infrastructure.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

	@InjectMocks
	private GlobalExceptionHandler testSubject;

	@Test
	void handleNotFound_returns404() {
		// GIVEN
		NotFoundException mockException = new NotFoundException("Asset not found");

		// WHEN
		ResponseEntity<Map<String, Object>> expectedResponse = testSubject.handleNotFound(
			mockException
		);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(expectedResponse.getBody()).containsEntry("message", "Asset not found");
		assertThat(expectedResponse.getBody()).containsEntry("status", 404);
	}

	@Test
	void handleAccessDenied_returns403() {
		// GIVEN
		AccessDeniedException mockException = new AccessDeniedException(
			"Asset does not belong to user"
		);

		// WHEN
		ResponseEntity<Map<String, Object>> expectedResponse = testSubject.handleAccessDenied(
			mockException
		);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(expectedResponse.getBody()).containsEntry(
			"message",
			"Asset does not belong to user"
		);
		assertThat(expectedResponse.getBody()).containsEntry("status", 403);
	}

	@Test
	void handleValidation_returns400() {
		// GIVEN
		ValidationException mockException = new ValidationException("Invalid amount");

		// WHEN
		ResponseEntity<Map<String, Object>> expectedResponse = testSubject.handleValidation(
			mockException
		);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(expectedResponse.getBody()).containsEntry("message", "Invalid amount");
		assertThat(expectedResponse.getBody()).containsEntry("status", 400);
	}

	@Test
	void handleIllegalArgument_returns400() {
		// GIVEN
		IllegalArgumentException mockException = new IllegalArgumentException("Bad input");

		// WHEN
		ResponseEntity<Map<String, Object>> expectedResponse = testSubject.handleIllegalArgument(
			mockException
		);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(expectedResponse.getBody()).containsEntry("message", "Bad input");
	}

	@Test
	void handleGeneric_returns500_withoutExposingInternalMessage() {
		// GIVEN
		Exception mockException = new Exception("Internal details");

		// WHEN
		ResponseEntity<Map<String, Object>> expectedResponse = testSubject.handleGeneric(mockException);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(expectedResponse.getBody()).containsEntry("message", "An unexpected error occurred");
		assertThat(expectedResponse.getBody()).containsEntry("status", 500);
	}
}
