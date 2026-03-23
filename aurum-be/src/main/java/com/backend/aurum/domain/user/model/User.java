package com.backend.aurum.domain.user.model;

import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.AssetCategory;
import com.backend.aurum.domain.user.enums.Currency;
import com.backend.aurum.domain.user.enums.Locale;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Currency currency;

	@Column(nullable = false)
	private Locale locale;

	@Column(columnDefinition = "TEXT")
	private String picture;

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonManagedReference
	private List<Asset> assets = new ArrayList<>();

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonManagedReference
	private List<AssetCategory> categories = new ArrayList<>();
}
