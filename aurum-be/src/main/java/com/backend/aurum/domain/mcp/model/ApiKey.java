package com.backend.aurum.domain.mcp.model;

import com.backend.aurum.domain.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "api_keys")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKey {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, unique = true)
	private String keyHash;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	private LocalDateTime lastUsedAt;
}
