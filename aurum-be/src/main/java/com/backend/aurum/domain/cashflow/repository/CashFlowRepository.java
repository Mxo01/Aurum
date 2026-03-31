package com.backend.aurum.domain.cashflow.repository;

import com.backend.aurum.domain.cashflow.model.CashFlow;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CashFlowRepository extends JpaRepository<CashFlow, UUID> {
	List<CashFlow> findByUserIdAndYearOrderByMonthAsc(UUID userId, Integer year);

	Optional<CashFlow> findByUserIdAndYearAndMonth(UUID userId, Integer year, Integer month);

	@Query(
		"SELECT DISTINCT cf.year FROM CashFlow cf WHERE cf.user.id = :userId ORDER BY cf.year DESC"
	)
	List<Integer> findDistinctYearsByUserId(@Param("userId") UUID userId);
}
