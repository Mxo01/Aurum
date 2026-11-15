package com.backend.aurum.domain.user.dto;

import com.backend.aurum.domain.user.enums.Currency;
import com.backend.aurum.domain.user.enums.Locale;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.Data;

@Data
@Schema(requiredProperties = { "id", "currency", "locale" })
public class UserDTO {

	private UUID id;
	private Currency currency;
	private Locale locale;

	@Schema(nullable = true, description = "Base64-encoded JPEG, absent when the user has no picture")
	private String picture;
}
