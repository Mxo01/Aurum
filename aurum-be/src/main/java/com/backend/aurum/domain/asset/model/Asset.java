package com.backend.aurum.domain.asset.model;

import com.backend.aurum.domain.user.enums.Currency;
import com.backend.aurum.domain.user.model.User;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "assets")
@Getter
@Setter
@NoArgsConstructor
public class Asset {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	@JsonBackReference
	private User user;

	@Column(nullable = false, length = 100)
	private String name;

	@ManyToOne(optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private AssetCategory category;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 3)
	private Currency originalCurrency = Currency.EUR;

	@Column(nullable = false)
	private Boolean isFavorite = false;

	@Enumerated(EnumType.STRING)
	@Column(name = "liability_type")
	private LiabilityType liabilityType;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_frequency")
	private PaymentFrequency paymentFrequency;

	@Column(name = "payment_amount", precision = 19, scale = 4)
	private BigDecimal paymentAmount;

	@Column(name = "next_due_date")
	private LocalDate nextDueDate;

	@Column(name = "payment_anchor_day")
	private Short paymentAnchorDay;

	@Column(name = "settled_at")
	private LocalDateTime settledAt;

	@Version
	@Column(nullable = false)
	private Long version;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@OneToMany(mappedBy = "asset", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("referenceDate ASC")
	@JsonManagedReference
	private List<Snapshot> snapshots = new ArrayList<>();

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
	}

	/**
	 * A liability is an asset that sits in a category the user declared as {@code LIABILITY}; the
	 * category is the user's intent, and it is what the dashboard, the totals and the analytics
	 * classify on. `liability_type` and `payment_amount` describe payment behaviour attached to
	 * that choice, so they never decide it: a legacy `liability_type = 'MANUAL'` on a row in an
	 * `ASSET` category turned twelve real assets into liabilities overnight.
	 *
	 * <p>Null-safe because {@code category} is a lazy {@code @ManyToOne} and an entity built in
	 * memory (mapper output, Instancio tests) may carry no category; reload paths must fetch it via
	 * {@code @EntityGraph} to stay inside the persistence context. Ignored by Jackson to keep the
	 * entity's JSON shape unchanged.
	 */
	@JsonIgnore
	public boolean isLiability() {
		return category != null && category.getType() == AssetType.LIABILITY;
	}
}
