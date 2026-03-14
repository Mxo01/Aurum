package com.backend.aurum.domain.user.service;

import com.auth0.client.mgmt.ManagementAPI;
import com.auth0.exception.Auth0Exception;
import com.backend.aurum.domain.auth.service.Auth0ManagementService;
import com.backend.aurum.domain.user.dto.UpdateCurrencyDTO;
import com.backend.aurum.domain.user.dto.UpdateNameDTO;
import com.backend.aurum.domain.user.enums.Currency;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
	private final UserRepository userRepository;
	private final Auth0ManagementService auth0ManagementService;

	public User findOrCreate(String jwtId, String email) {
		return userRepository
				.findByJwtId(jwtId)
				.orElseGet(() -> {
					try {
						User newUser = new User();
						newUser.setJwtId(jwtId);
						newUser.setEmail(email);
						newUser.setCurrency(Currency.EUR);
						return userRepository.saveAndFlush(newUser);
					} catch (Exception e) {
						return userRepository.findByJwtId(jwtId)
								.orElseThrow(() -> new RuntimeException("Failed to find or create user", e));
					}
				});
	}

	@Transactional
	public User getUserById(UUID userId) {
		return userRepository.findById(userId).orElseThrow();
	}

	@Transactional
	public void deleteUser(UUID userId, String jwtId) {
		userRepository.deleteById(userId);

		try {
			ManagementAPI managementApi = auth0ManagementService.getManagementAPI();
			managementApi.users().delete(jwtId).execute();
		} catch (Auth0Exception e) {
			throw new RuntimeException("Errore durante l'eliminazione da Auth0", e);
		}
	}

	@Transactional
	public void updateAuth0Name(String jwtId, UpdateNameDTO dto) {
		try {
			ManagementAPI mgmt = auth0ManagementService.getManagementAPI();
			com.auth0.json.mgmt.users.User auth0User = new com.auth0.json.mgmt.users.User();
			auth0User.setName(dto.name());
			mgmt.users().update(jwtId, auth0User).execute();
		} catch (Auth0Exception e) {
			throw new RuntimeException("Errore durante l'aggiornamento del nome su Auth0: " + e.getMessage());
		}
	}

	@Transactional
	public void updateCurrency(UUID userId, UpdateCurrencyDTO dto) {
		User user = userRepository.findById(userId).orElseThrow();
		user.setCurrency(dto.currency());
		userRepository.save(user);
	}
}
