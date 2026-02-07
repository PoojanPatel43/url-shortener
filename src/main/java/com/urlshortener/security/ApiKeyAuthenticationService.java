package com.urlshortener.security;

import com.urlshortener.entity.ApiKey;
import com.urlshortener.repository.ApiKeyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class ApiKeyAuthenticationService {

    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;

    public ApiKeyAuthenticationService(ApiKeyRepository apiKeyRepository,
                                        @Lazy PasswordEncoder passwordEncoder) {
        this.apiKeyRepository = apiKeyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserDetails authenticateApiKey(String rawApiKey) {
        // API key format: prefix_randomString
        if (rawApiKey == null || !rawApiKey.contains("_")) {
            return null;
        }

        String keyHash = hashApiKey(rawApiKey);

        return apiKeyRepository.findByKeyHash(keyHash)
                .filter(ApiKey::getEnabled)
                .filter(key -> !key.isExpired())
                .map(apiKey -> {
                    // Update last used timestamp
                    apiKeyRepository.updateLastUsedAt(apiKey.getId(), LocalDateTime.now());
                    log.debug("API key authenticated: {}", apiKey.getPrefix());
                    return new CustomUserDetails(apiKey.getUser());
                })
                .orElse(null);
    }

    public String hashApiKey(String rawApiKey) {
        // Use a simple hash for API key lookup (not the password encoder)
        // This allows for O(1) lookup in the database
        return String.valueOf(rawApiKey.hashCode());
    }

    public String generateSecureHash(String rawApiKey) {
        return passwordEncoder.encode(rawApiKey);
    }
}
