package com.backend.aurum.domain.mcp.repository;

import com.backend.aurum.domain.mcp.model.ApiKey;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
	@Query("SELECT ak FROM ApiKey ak JOIN FETCH ak.user WHERE ak.keyHash = :keyHash")
	Optional<ApiKey> findByKeyHash(@Param("keyHash") String keyHash);

	List<ApiKey> findByUserId(UUID userId);

	void deleteByUserId(UUID userId);
}
