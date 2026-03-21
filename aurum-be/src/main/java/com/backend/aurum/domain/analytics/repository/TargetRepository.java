package com.backend.aurum.domain.analytics.repository;

import com.backend.aurum.domain.analytics.model.Target;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TargetRepository extends JpaRepository<Target, UUID> {
	List<Target> findByUserId(UUID userId);
}
