package com.backend.aurum.domain.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, unique = true)
	private String jwtId;

	@Column(unique = true)
	private String email;

	@Column(name = "first_name", nullable = true)
	private String firstName;

	@Column(name = "last_name", nullable = true)
	private String lastName;
}
