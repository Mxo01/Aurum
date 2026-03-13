package com.backend.aurum.domain.auth.service;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth0.client.auth.AuthAPI;
import com.auth0.client.mgmt.ManagementAPI;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.auth.TokenHolder;
import com.auth0.net.TokenRequest;

@Service
public class Auth0ManagementService {

	@Value("${auth0.management.domain}")
	private String domain;
	@Value("${auth0.management.clientId}")
	private String clientId;
	@Value("${auth0.management.clientSecret}")
	private String clientSecret;

	private String cachedToken;
	private Instant expiresAt;

	public String getAccessToken() throws Auth0Exception {
		if (cachedToken != null && expiresAt != null && Instant.now().isBefore(expiresAt.minusSeconds(60))) {
			return cachedToken;
		}

		AuthAPI authAPI = AuthAPI.newBuilder(domain, clientId, clientSecret).build();
		TokenRequest tokenRequest = authAPI.requestToken("https://" + domain + "/api/v2/");
		TokenHolder holder = tokenRequest.execute().getBody();

		this.cachedToken = holder.getAccessToken();
		this.expiresAt = Instant.now().plusSeconds(holder.getExpiresIn());

		return cachedToken;
	}

	public ManagementAPI getManagementAPI() throws Auth0Exception {
		return ManagementAPI.newBuilder(domain, getAccessToken()).build();
	}
}
