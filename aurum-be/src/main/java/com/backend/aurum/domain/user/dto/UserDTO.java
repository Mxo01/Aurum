package com.backend.aurum.domain.user.dto;

import java.util.UUID;

import lombok.Data;

@Data
public class UserDTO {
	private UUID id;
	private String email;
}
