package com.backend.aurum.domain.analytics.repository;

import com.backend.aurum.domain.analytics.model.Target;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TargetRepository extends JpaRepository<Target, UUID> {
    List<Target> findByUserId(UUID userId);
}
