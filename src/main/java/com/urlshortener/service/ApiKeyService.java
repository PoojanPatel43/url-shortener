package com.urlshortener.service;

import com.urlshortener.dto.ApiKeyRequest;
import com.urlshortener.dto.ApiKeyResponse;
import com.urlshortener.entity.ApiKey;
import com.urlshortener.entity.User;
import com.urlshortener.exception.BadRequestException;
import com.urlshortener.exception.ResourceNotFoundException;
import com.urlshortener.repository.ApiKeyRepository;
import com.urlshortener.security.ApiKeyAuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private static final int MAX_API_KEYS_PER_USER = 5;
    private static final int API_KEY_LENGTH = 32;
    private static final String API_KEY_PREFIX = "ush";

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyAuthenticationService apiKeyAuthenticationService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public ApiKeyResponse createApiKey(ApiKeyRequest request, User user) {
        if (apiKeyRepository.countByUser(user) >= MAX_API_KEYS_PER_USER) {
            throw new BadRequestException("Maximum number of API keys (" + MAX_API_KEYS_PER_USER + ") reached");
        }

        String rawKey = generateApiKey();
        String fullKey = API_KEY_PREFIX + "_" + rawKey;
        String keyHash = apiKeyAuthenticationService.hashApiKey(fullKey);

        LocalDateTime expiresAt = null;
        if (request.getExpirationDays() != null && request.getExpirationDays() > 0) {
            expiresAt = LocalDateTime.now().plusDays(request.getExpirationDays());
        }

        ApiKey apiKey = ApiKey.builder()
                .user(user)
                .name(request.getName())
                .keyHash(keyHash)
                .prefix(API_KEY_PREFIX)
                .expiresAt(expiresAt)
                .build();

        apiKey = apiKeyRepository.save(apiKey);
        log.info("Created API key '{}' for user: {}", request.getName(), user.getEmail());

        // Return with the full key (only shown once)
        return mapToResponse(apiKey, fullKey);
    }

    @Transactional(readOnly = true)
    public List<ApiKeyResponse> getUserApiKeys(User user) {
        return apiKeyRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(key -> mapToResponse(key, null))
                .collect(Collectors.toList());
    }

    @Transactional
    public void revokeApiKey(Long keyId, User user) {
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new ResourceNotFoundException("API key not found"));

        if (!apiKey.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You don't have permission to revoke this API key");
        }

        apiKey.setEnabled(false);
        apiKeyRepository.save(apiKey);
        log.info("Revoked API key '{}' for user: {}", apiKey.getName(), user.getEmail());
    }

    @Transactional
    public void deleteApiKey(Long keyId, User user) {
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new ResourceNotFoundException("API key not found"));

        if (!apiKey.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You don't have permission to delete this API key");
        }

        apiKeyRepository.delete(apiKey);
        log.info("Deleted API key '{}' for user: {}", apiKey.getName(), user.getEmail());
    }

    private String generateApiKey() {
        byte[] bytes = new byte[API_KEY_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private ApiKeyResponse mapToResponse(ApiKey apiKey, String fullKey) {
        return ApiKeyResponse.builder()
                .id(apiKey.getId())
                .name(apiKey.getName())
                .prefix(apiKey.getPrefix())
                .key(fullKey)  // Only set when creating
                .enabled(apiKey.getEnabled())
                .lastUsedAt(apiKey.getLastUsedAt())
                .expiresAt(apiKey.getExpiresAt())
                .createdAt(apiKey.getCreatedAt())
                .build();
    }
}
