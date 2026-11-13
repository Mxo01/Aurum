package com.backend.aurum.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.Data;

@Data
@Schema(requiredProperties = { "id" })
public class UserDTO {

	private UUID id;
}
