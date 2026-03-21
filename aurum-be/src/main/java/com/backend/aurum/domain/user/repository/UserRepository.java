package com.backend.aurum.domain.user.repository;

import com.backend.aurum.domain.user.model.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
	Optional<User> findByJwtId(String jwtId);
}
