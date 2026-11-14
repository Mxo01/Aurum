package com.backend.aurum.domain.asset.repository;

import com.backend.aurum.domain.asset.model.SchedulerRun;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchedulerRunRepository extends JpaRepository<SchedulerRun, UUID> {}
