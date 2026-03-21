package com.backend.aurum.domain.asset.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "snapshots")
@Getter
@Setter
@NoArgsConstructor
public class Snapshot {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "asset_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	@JsonBackReference
	private Asset asset;

	@Column(nullable = false)
	private LocalDate referenceDate;

	@Column(nullable = false, precision = 19, scale = 4)
	private BigDecimal amountOriginalCurrency;

	@Column(nullable = false, precision = 19, scale = 6)
	private BigDecimal exchangeRateToBase = BigDecimal.ONE.setScale(6, RoundingMode.HALF_UP);

	public BigDecimal getAmountInBaseCurrency() {
		if (amountOriginalCurrency == null || exchangeRateToBase == null) {
			return BigDecimal.ZERO;
		}
		return amountOriginalCurrency.multiply(exchangeRateToBase).setScale(4, RoundingMode.HALF_UP);
	}
}
