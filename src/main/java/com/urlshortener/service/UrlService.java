package com.urlshortener.service;

import com.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.entity.Url;
import com.urlshortener.entity.User;
import com.urlshortener.exception.BadRequestException;
import com.urlshortener.exception.ResourceNotFoundException;
import com.urlshortener.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UrlService {

    private final UrlRepository urlRepository;
    private final Base62Encoder base62Encoder;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.short-url-length:7}")
    private int shortUrlLength;

    @Value("${app.url.default-expiration-days:365}")
    private int defaultExpirationDays;

    @Value("${app.url.max-custom-alias-length:20}")
    private int maxCustomAliasLength;

    private static final UrlValidator URL_VALIDATOR = new UrlValidator(new String[]{"http", "https"});

    @Transactional
    public UrlResponse createShortUrl(CreateUrlRequest request, User user) {
        validateUrl(request.getUrl());

        String shortCode;
        if (request.getCustomAlias() != null && !request.getCustomAlias().isBlank()) {
            shortCode = validateAndGetCustomAlias(request.getCustomAlias());
        } else {
            shortCode = generateUniqueShortCode();
        }

        LocalDateTime expiresAt = null;
        if (request.getExpirationDays() != null && request.getExpirationDays() > 0) {
            expiresAt = LocalDateTime.now().plusDays(request.getExpirationDays());
        } else if (defaultExpirationDays > 0) {
            expiresAt = LocalDateTime.now().plusDays(defaultExpirationDays);
        }

        Url url = Url.builder()
                .shortCode(shortCode)
                .originalUrl(request.getUrl())
                .user(user)
                .customAlias(request.getCustomAlias() != null && !request.getCustomAlias().isBlank())
                .expiresAt(expiresAt)
                .build();

        url = urlRepository.save(url);
        log.info("Created short URL: {} -> {}", shortCode, request.getUrl());

        return mapToResponse(url);
    }

    @Cacheable(value = "urls", key = "#shortCode")
    @Transactional(readOnly = true)
    public String getOriginalUrl(String shortCode) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("URL", "shortCode", shortCode));

        if (!url.getIsActive()) {
            throw new ResourceNotFoundException("URL has been deactivated");
        }

        if (url.isExpired()) {
            throw new ResourceNotFoundException("URL has expired");
        }

        return url.getOriginalUrl();
    }

    @Transactional(readOnly = true)
    public Url getUrlByShortCode(String shortCode) {
        return urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("URL", "shortCode", shortCode));
    }

    @Transactional(readOnly = true)
    public UrlResponse getUrlDetails(String shortCode, User user) {
        Url url = getUrlByShortCode(shortCode);
        validateOwnership(url, user);
        return mapToResponse(url);
    }

    @Transactional(readOnly = true)
    public Page<UrlResponse> getUserUrls(User user, Pageable pageable) {
        return urlRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(this::mapToResponse);
    }

    @CacheEvict(value = "urls", key = "#shortCode")
    @Transactional
    public UrlResponse updateUrl(String shortCode, CreateUrlRequest request, User user) {
        Url url = getUrlByShortCode(shortCode);
        validateOwnership(url, user);

        if (request.getUrl() != null && !request.getUrl().isBlank()) {
            validateUrl(request.getUrl());
            url.setOriginalUrl(request.getUrl());
        }

        if (request.getExpirationDays() != null) {
            url.setExpiresAt(LocalDateTime.now().plusDays(request.getExpirationDays()));
        }

        url = urlRepository.save(url);
        log.info("Updated URL: {} by user: {}", shortCode, user.getEmail());
        return mapToResponse(url);
    }

    @CacheEvict(value = "urls", key = "#shortCode")
    @Transactional
    public void deleteUrl(String shortCode, User user) {
        Url url = getUrlByShortCode(shortCode);
        validateOwnership(url, user);
        url.setIsActive(false);
        urlRepository.save(url);
        log.info("Deactivated URL: {}", shortCode);
    }

    @Transactional
    public void incrementClickCount(Long urlId) {
        urlRepository.incrementClickCount(urlId);
    }

    @Scheduled(cron = "0 0 * * * *") // Run every hour
    @Transactional
    public void deactivateExpiredUrls() {
        int count = urlRepository.deactivateExpiredUrls(LocalDateTime.now());
        if (count > 0) {
            log.info("Deactivated {} expired URLs", count);
        }
    }

    private void validateUrl(String url) {
        if (!URL_VALIDATOR.isValid(url)) {
            throw new BadRequestException("Invalid URL format: " + url);
        }
    }

    private String validateAndGetCustomAlias(String alias) {
        if (alias.length() > maxCustomAliasLength) {
            throw new BadRequestException("Custom alias must not exceed " + maxCustomAliasLength + " characters");
        }

        if (!base62Encoder.isValid(alias)) {
            throw new BadRequestException("Custom alias can only contain alphanumeric characters");
        }

        if (urlRepository.existsByShortCode(alias)) {
            throw new BadRequestException("Custom alias is already taken: " + alias);
        }

        return alias;
    }

    private String generateUniqueShortCode() {
        String shortCode;
        int attempts = 0;
        do {
            shortCode = base62Encoder.generateRandom(shortUrlLength);
            attempts++;
            if (attempts > 10) {
                throw new RuntimeException("Failed to generate unique short code after 10 attempts");
            }
        } while (urlRepository.existsByShortCode(shortCode));
        return shortCode;
    }

    private void validateOwnership(Url url, User user) {
        if (url.getUser() == null || !url.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You don't have permission to access this URL");
        }
    }

    private UrlResponse mapToResponse(Url url) {
        return UrlResponse.builder()
                .id(url.getId())
                .shortCode(url.getShortCode())
                .shortUrl(baseUrl + "/" + url.getShortCode())
                .originalUrl(url.getOriginalUrl())
                .clickCount(url.getClickCount())
                .customAlias(url.getCustomAlias())
                .isActive(url.getIsActive())
                .expiresAt(url.getExpiresAt())
                .createdAt(url.getCreatedAt())
                .build();
    }
}
