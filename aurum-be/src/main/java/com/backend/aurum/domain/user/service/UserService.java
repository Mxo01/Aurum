package com.backend.aurum.domain.user.service;

import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;

	@Transactional
	@Cacheable(value = "users", key = "#jwtId")
	public User findOrCreate(String jwtId, String email) {
		return userRepository
				.findByJwtId(jwtId)
				.orElseGet(() -> {
					User newUser = new User();
					newUser.setJwtId(jwtId);
					newUser.setEmail(email);
					return userRepository.save(newUser);
				});
	}
}
