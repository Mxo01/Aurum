package com.backend.aurum.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SecurityUtils {

    public UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
				if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        
				Object principal = authentication.getPrincipal();
        
				if (principal instanceof UUID) {
            return (UUID) principal;
        }
    
				try {
            return UUID.fromString(principal.toString());
        } catch (Exception e) {
            throw new RuntimeException("Invalid principal type: " + principal.getClass().getName());
        }
    }
}
