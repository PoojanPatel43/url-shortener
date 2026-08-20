package com.urlshortener.security;

import com.urlshortener.entity.ApiKey;
import com.urlshortener.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyAuthenticationService {

    private final ApiKeyRepository apiKeyRepository;

    @Transactional
    public UserDetails authenticateApiKey(String rawApiKey) {
        // API key format: prefix_randomString
        if (rawApiKey == null || !rawApiKey.contains("_")) {
            log.debug("Invalid API key format");
            return null;
        }

        String keyHash = hashApiKey(rawApiKey);

        return apiKeyRepository.findByKeyHash(keyHash)
                .filter(ApiKey::getEnabled)
                .filter(key -> !key.isExpired())
                .filter(key -> {
                    if (!key.getUser().getEnabled()) {
                        log.warn("API key auth rejected for disabled user: {}", key.getUser().getEmail());
                        return false;
                    }
                    return true;
                })
                .map(apiKey -> {
                    apiKeyRepository.updateLastUsedAt(apiKey.getId(), LocalDateTime.now());
                    log.debug("API key authenticated: {}", apiKey.getPrefix());
                    return new CustomUserDetails(apiKey.getUser());
                })
                .orElseGet(() -> {
                    log.warn("API key authentication failed");
                    return null;
                });
    }

    public String hashApiKey(String rawApiKey) {
        return String.valueOf(rawApiKey.hashCode());
    }
}
